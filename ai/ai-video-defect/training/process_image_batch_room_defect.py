"""Classify ordered server-sampled images by room, then run two-stage YOLO."""

from __future__ import annotations

import argparse
import json
import time
from pathlib import Path
from typing import Any

from training.gemini_room_classifier import GeminiRoomClassifier, stabilize_room_predictions
from training.process_images_two_stage import IMAGE_SUFFIXES, process_images


def display_path(path: Path, root: Path) -> str:
    try:
        return str(path.relative_to(root))
    except ValueError:
        return str(path)


def discover_images(input_path: Path) -> list[Path]:
    if input_path.is_file() and input_path.suffix.lower() in IMAGE_SUFFIXES:
        return [input_path]
    if input_path.is_dir():
        return sorted(
            path for path in input_path.iterdir()
            if path.is_file() and path.suffix.lower() in IMAGE_SUFFIXES
        )
    raise FileNotFoundError(f"Image file or directory not found: {input_path}")


def load_manifest(manifest_path: Path | None) -> dict[str, dict[str, Any]]:
    if manifest_path is None:
        return {}
    payload = json.loads(manifest_path.read_text(encoding="utf-8"))
    items = payload.get("images") if isinstance(payload, dict) else None
    if not isinstance(items, list):
        raise ValueError("Manifest must contain an images array")
    metadata: dict[str, dict[str, Any]] = {}
    for item in items:
        if not isinstance(item, dict) or not item.get("filename"):
            raise ValueError("Every manifest image needs a filename")
        filename = str(item["filename"])
        if filename in metadata:
            raise ValueError(f"Duplicate filename in manifest: {filename}")
        metadata[filename] = item
    return metadata


def build_image_rows(
    image_paths: list[Path], manifest: dict[str, dict[str, Any]]
) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for sequence_id, image_path in enumerate(image_paths, start=1):
        metadata = manifest.get(image_path.name, {})
        timestamp = metadata.get("timestampSec")
        if timestamp is not None:
            timestamp = float(timestamp)
        rows.append({
            "frameId": sequence_id,
            "sequenceIndex": sequence_id - 1,
            "imageId": str(metadata.get("imageId", f"image-{sequence_id:04d}")),
            "timestampSec": timestamp,
            "filename": image_path.name,
            "imagePath": str(image_path),
        })
    unknown_manifest_files = sorted(set(manifest) - {path.name for path in image_paths})
    if unknown_manifest_files:
        raise ValueError(f"Manifest references missing images: {unknown_manifest_files}")
    return rows


def classify_rooms(args: argparse.Namespace, rows: list[dict[str, Any]]) -> dict[str, Any]:
    if args.room_provider == "disabled":
        return {
            "results": [{
                "frameId": row["frameId"],
                "timestampSec": row["timestampSec"],
                "filename": row["filename"],
                "room": "unknown",
                "uncertain": True,
                "provider": "disabled",
                "model": None,
            } for row in rows],
            "apiCalls": 0,
            "errors": [],
            "elapsedSec": 0.0,
        }
    classifier = GeminiRoomClassifier(
        model=args.gemini_model,
        batch_size=args.room_batch_size,
        max_image_size=args.room_image_size,
        retries=args.room_retries,
    )
    return classifier.classify(rows)


def process_image_batch(args: argparse.Namespace) -> dict[str, Any]:
    root = Path(__file__).resolve().parents[1]
    input_path = (root / args.input).resolve()
    output = (root / args.output).resolve()
    manifest_path = (root / args.manifest).resolve() if args.manifest else None
    if manifest_path is not None and not manifest_path.is_file():
        raise FileNotFoundError(f"Manifest not found: {manifest_path}")
    image_paths = discover_images(input_path)
    if not image_paths:
        raise ValueError(f"No supported images found: {input_path}")

    started = time.perf_counter()
    rows = build_image_rows(image_paths, load_manifest(manifest_path))
    room_result = classify_rooms(args, rows)
    stabilized, room_segments = stabilize_room_predictions(
        room_result["results"], args.room_window_size, args.room_min_votes
    )
    room_by_filename = {row["filename"]: row for row in stabilized}
    metadata_by_filename = {row["filename"]: row for row in rows}
    metadata_by_sequence_id = {row["frameId"]: row for row in rows}
    for segment in room_segments:
        start = metadata_by_sequence_id[segment["startFrameId"]]
        end = metadata_by_sequence_id[segment["endFrameId"]]
        segment.update({
            "startImageId": start["imageId"],
            "endImageId": end["imageId"],
            "startSequenceIndex": start["sequenceIndex"],
            "endSequenceIndex": end["sequenceIndex"],
        })

    defect_output = output / "defect_analysis"
    defect_args = argparse.Namespace(
        input=input_path,
        job_id=args.job_id,
        # The nested two-stage runner still exposes its legacy single-media
        # metadata fields. Batch correlation is performed with each manifest
        # imageId below, so use the inspection job id only as the parent value.
        media_id=args.job_id,
        model_version="two_stage_negative_rot4",
        output=defect_output,
        binary=args.binary,
        multiclass=args.multiclass,
        multiclass_imgsz=args.multiclass_imgsz,
        binary_imgsz=args.binary_imgsz,
        confidence=args.confidence,
        nms_iou=args.nms_iou,
        merge_iou=args.merge_iou,
        defect_verifier=args.defect_verifier,
        gemini_defect_model=args.gemini_defect_model,
        gemini_defect_retries=args.gemini_defect_retries,
        gemini_reject_confidence=args.gemini_reject_confidence,
    )
    defects = process_images(defect_args)

    images: list[dict[str, Any]] = []
    internal_id_to_image: dict[str, dict[str, Any]] = {}
    for image_result in defects["images"]:
        room = room_by_filename[image_result["filename"]]
        metadata = metadata_by_filename[image_result["filename"]]
        external_image_id = metadata["imageId"]
        enriched = {
            **image_result,
            "imageId": external_image_id,
            "sequenceIndex": metadata["sequenceIndex"],
            "timestampSec": metadata["timestampSec"],
            "room": {
                "raw": room["room"],
                "stable": room["stableRoom"],
                "uncertain": room["uncertain"],
                "containsPerson": room.get("containsPerson", True),
                "provider": room["provider"],
                "model": room["model"],
                "roomSegmentId": room["roomSegmentId"],
            },
        }
        images.append(enriched)
        internal_id_to_image[image_result["imageId"]] = enriched

    observations: list[dict[str, Any]] = []
    for observation in defects["observations"]:
        image = internal_id_to_image[observation["imageId"]]
        observations.append({
            **observation,
            "imageId": image["imageId"],
            "representativeImageId": image["imageId"],
            "sequenceIndex": image["sequenceIndex"],
            "timestampSec": image["timestampSec"],
            "room": image["room"]["stable"],
            "roomSegmentId": image["room"]["roomSegmentId"],
        })

    payload = {
        "jobId": args.job_id,
        "modelVersion": defects["modelVersion"],
        "status": "completed",
        "input": {
            "path": display_path(input_path, root),
            "manifestPath": display_path(manifest_path, root) if manifest_path else None,
            "imageCount": len(images),
            "samplingAndQualityFiltering": "completed_by_backend",
        },
        "processing": {
            "mode": "backend_sampled_images_room_gemini_yolo",
            "processedCount": len(images),
            "elapsedSec": round(time.perf_counter() - started, 3),
            "roomElapsedSec": room_result["elapsedSec"],
            "defectElapsedSec": defects["processing"]["elapsedSec"],
            "device": defects["processing"]["device"],
        },
        "roomClassification": {
            "provider": args.room_provider,
            "model": args.gemini_model if args.room_provider == "gemini" else None,
            "batchSize": args.room_batch_size,
            "imageSize": args.room_image_size,
            "windowSize": args.room_window_size,
            "minVotes": args.room_min_votes,
            "apiCalls": room_result["apiCalls"],
            "errors": room_result["errors"],
        },
        "roomSegments": room_segments,
        "images": images,
        "observations": observations,
    }
    output.mkdir(parents=True, exist_ok=True)
    (output / "result.json").write_text(
        json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    return payload


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Classify backend-sampled images by room, then run two-stage YOLO."
    )
    parser.add_argument("--input", type=Path, required=True, help="Image file or ordered image directory")
    parser.add_argument("--manifest", type=Path, help="Optional JSON metadata for image IDs and timestamps")
    parser.add_argument("--job-id", default="local-image-room-test")
    parser.add_argument("--output", type=Path, default=Path("reports/image_room_defect_test"))
    parser.add_argument("--room-provider", choices=("gemini", "disabled"), default="gemini")
    parser.add_argument("--gemini-model", default="gemini-3.5-flash-lite")
    parser.add_argument("--room-batch-size", type=int, default=10)
    parser.add_argument("--room-image-size", type=int, default=384)
    parser.add_argument("--room-retries", type=int, default=3)
    parser.add_argument("--room-window-size", type=int, default=5)
    parser.add_argument("--room-min-votes", type=int, default=3)
    parser.add_argument("--binary", type=Path, default=Path("models/active/two_stage_negative_rot4/binary/best.pt"))
    parser.add_argument("--multiclass", type=Path, default=Path("models/active/two_stage_negative_rot4/multiclass/best.pt"))
    parser.add_argument("--multiclass-imgsz", type=int, default=320)
    parser.add_argument("--binary-imgsz", type=int, default=640)
    parser.add_argument("--confidence", type=float, default=0.0325)
    parser.add_argument("--nms-iou", type=float, default=0.40)
    parser.add_argument("--merge-iou", type=float, default=0.30)
    parser.add_argument("--defect-verifier", choices=("disabled", "gemini"), default="gemini")
    parser.add_argument("--gemini-defect-model", default="gemini-3.5-flash-lite")
    parser.add_argument("--gemini-defect-retries", type=int, default=3)
    parser.add_argument("--gemini-reject-confidence", type=float, default=0.90)
    return parser


def main() -> None:
    args = build_parser().parse_args()
    try:
        payload = process_image_batch(args)
    except Exception as error:
        root = Path(__file__).resolve().parents[1]
        output = (root / args.output).resolve()
        output.mkdir(parents=True, exist_ok=True)
        failed = {
            "jobId": args.job_id,
            "status": "failed",
            "error": {"type": type(error).__name__, "message": str(error)},
        }
        (output / "result.json").write_text(
            json.dumps(failed, ensure_ascii=False, indent=2), encoding="utf-8"
        )
        print(json.dumps(failed, ensure_ascii=False))
        raise SystemExit(1) from error
    print(json.dumps({
        "output": str((Path(__file__).resolve().parents[1] / args.output).resolve()),
        "jobId": payload["jobId"],
        "status": payload["status"],
        "processedCount": payload["processing"]["processedCount"],
        "roomSegmentCount": len(payload["roomSegments"]),
        "observationCount": len(payload["observations"]),
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
