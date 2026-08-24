"""Process server-provided images with full-image Binary + multiclass YOLO."""

from __future__ import annotations

import argparse
import json
import shutil
import time
from pathlib import Path

import cv2
import torch
from ultralytics import YOLO

from training.predict_dacon_two_stage import box_iou


IMAGE_SUFFIXES = {".jpg", ".jpeg"}


def portable_path(path: Path, root: Path) -> str:
    try:
        return str(path.relative_to(root))
    except ValueError:
        return str(path)


def predict_image(model: YOLO, image_path: Path, imgsz: int, confidence: float, nms_iou: float, device: str) -> list[dict]:
    result = next(iter(model.predict(
        source=str(image_path), imgsz=imgsz, conf=confidence, iou=nms_iou,
        max_det=300, device=device, stream=True, verbose=False,
    )))
    detections = []
    if result.boxes is None:
        return detections
    names = result.names
    for box, cls_id, conf in zip(
        result.boxes.xyxy.cpu().tolist(),
        result.boxes.cls.cpu().tolist(),
        result.boxes.conf.cpu().tolist(),
    ):
        class_id = int(cls_id)
        detections.append({
            "classId": class_id,
            "label": names.get(class_id, str(class_id)),
            "confidence": round(float(conf), 6),
            "box": {
                "left": round(float(box[0]), 2),
                "top": round(float(box[1]), 2),
                "right": round(float(box[2]), 2),
                "bottom": round(float(box[3]), 2),
            },
            "classificationStatus": "classified",
            "reviewStatus": "needs_review",
            "defectConfidence": round(float(conf), 6),
            "classConfidence": round(float(conf), 6),
        })
    return detections


def box_list(detection: dict) -> list[float]:
    box = detection["box"]
    return [box["left"], box["top"], box["right"], box["bottom"]]


def merge_detections(binary: list[dict], multiclass: list[dict], merge_iou: float) -> list[dict]:
    merged = [dict(item, source="multiclass") for item in multiclass]
    for binary_item in binary:
        if not any(box_iou(box_list(binary_item), box_list(multi_item)) >= merge_iou for multi_item in multiclass):
            merged.append({
                "classId": 12,
                "label": "other",
                "confidence": binary_item["confidence"],
                "box": binary_item["box"],
                "classificationStatus": "classified_as_other",
                "reviewStatus": "needs_review",
                "defectConfidence": binary_item["confidence"],
                "classConfidence": None,
                "source": "binary_only",
            })
    return sorted(merged, key=lambda item: item["confidence"], reverse=True)


def crop_image(image, box: dict, destination: Path, margin_ratio: float) -> tuple[int, int]:
    height, width = image.shape[:2]
    left, top, right, bottom = box["left"], box["top"], box["right"], box["bottom"]
    box_width, box_height = right - left, bottom - top
    margin_x, margin_y = box_width * margin_ratio, box_height * margin_ratio
    crop_left = max(0, int(left - margin_x))
    crop_top = max(0, int(top - margin_y))
    crop_right = min(width, int(right + margin_x))
    crop_bottom = min(height, int(bottom + margin_y))
    crop = image[crop_top:crop_bottom, crop_left:crop_right]
    if crop.size == 0:
        raise ValueError(f"Empty crop for box {box}")
    destination.parent.mkdir(parents=True, exist_ok=True)
    cv2.imwrite(str(destination), crop)
    return int(crop.shape[1]), int(crop.shape[0])


def process_images(args: argparse.Namespace) -> dict:
    root = Path(__file__).resolve().parents[1]
    input_path = (root / args.input).resolve()
    output = (root / args.output).resolve()
    binary_path = (root / args.binary).resolve()
    multiclass_path = (root / args.multiclass).resolve()
    model_version = args.model_version or multiclass_path.parent.parent.name
    if not input_path.is_file() or input_path.suffix.lower() not in IMAGE_SUFFIXES:
        raise FileNotFoundError(f"JPEG image not found: {input_path}")
    image_paths = [input_path]

    device = "0" if torch.cuda.is_available() else "cpu"
    binary_model = YOLO(str(binary_path))
    multiclass_model = YOLO(str(multiclass_path))
    image_results = []
    observations = []
    started = time.perf_counter()
    evidence_dir = output / "evidence"
    crop_dir = output / "crops"

    for image_index, image_path in enumerate(image_paths, start=1):
        image = cv2.imread(str(image_path))
        if image is None:
            raise ValueError(f"Unable to read image: {image_path}")
        height, width = image.shape[:2]
        multiclass = predict_image(multiclass_model, image_path, args.multiclass_imgsz, args.confidence, args.nms_iou, device)
        binary = predict_image(binary_model, image_path, args.binary_imgsz, args.confidence, args.nms_iou, device)
        merged = merge_detections(binary, multiclass, args.merge_iou)
        image_id = f"image-{image_index:04d}"
        evidence_path = None
        if merged:
            evidence_path = evidence_dir / image_path.name
            evidence_path.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(image_path, evidence_path)

        for detection_index, detection in enumerate(merged, start=1):
            observation_id = f"obs-{image_index:04d}-{detection_index:03d}"
            crop_path = crop_dir / f"{observation_id}_{detection['label']}.jpg"
            crop_width, crop_height = crop_image(image, detection["box"], crop_path, args.crop_margin_ratio)
            observation = dict(detection)
            observation.update({
                "observationGroupId": observation_id,
                "imageId": image_id,
                "filename": image_path.name,
                "representativeImageId": image_id,
                "evidencePath": portable_path(evidence_path, root) if evidence_path else None,
                "cropPath": portable_path(crop_path, root),
                "cropImage": {"width": crop_width, "height": crop_height},
            })
            observations.append(observation)

        image_results.append({
            "imageId": image_id,
            "filename": image_path.name,
            "image": {"width": width, "height": height},
            "detections": merged,
            "binaryDetections": binary,
            "multiclassDetections": multiclass,
            "evidencePath": portable_path(evidence_path, root) if evidence_path else None,
        })

    elapsed = time.perf_counter() - started
    payload = {
        "jobId": args.job_id,
        "mediaId": args.media_id,
        "modelVersion": model_version,
        "status": "completed",
        "input": {"path": portable_path(input_path, root), "imageCount": len(image_paths)},
        "processing": {
            "mode": "image_batch",
            "processedCount": len(image_results),
            "device": device,
            "elapsedSec": round(elapsed, 3),
            "confidence": args.confidence,
            "mergeIou": args.merge_iou,
            "cropMarginRatio": args.crop_margin_ratio,
        },
        "images": image_results,
        "observations": observations,
    }
    output.mkdir(parents=True, exist_ok=True)
    (output / "result.json").write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    return payload


def main() -> None:
    parser = argparse.ArgumentParser(description="Run full-image Binary + multiclass YOLO on server-provided images.")
    parser.add_argument("--input", type=Path, required=True, help="One JPEG file associated with --media-id")
    parser.add_argument("--job-id", default="local-image-test")
    parser.add_argument("--media-id", required=True, help="Server media UUID associated with this analysis job")
    parser.add_argument("--model-version", help="Deployed model version. Defaults to the active model directory name")
    parser.add_argument("--output", type=Path, default=Path("reports/image_two_stage_test"))
    parser.add_argument("--binary", type=Path, default=Path("models/active/two_stage_negative_rot4/binary/best.pt"))
    parser.add_argument("--multiclass", type=Path, default=Path("models/active/two_stage_negative_rot4/multiclass/best.pt"))
    parser.add_argument("--multiclass-imgsz", type=int, default=320)
    parser.add_argument("--binary-imgsz", type=int, default=640)
    parser.add_argument("--confidence", type=float, default=0.0325)
    parser.add_argument("--nms-iou", type=float, default=0.40)
    parser.add_argument("--merge-iou", type=float, default=0.30)
    parser.add_argument("--crop-margin-ratio", type=float, default=0.15)
    args = parser.parse_args()
    try:
        payload = process_images(args)
    except Exception as error:
        root = Path(__file__).resolve().parents[1]
        output = (root / args.output).resolve()
        output.mkdir(parents=True, exist_ok=True)
        failed = {"jobId": args.job_id, "status": "failed", "error": {"type": type(error).__name__, "message": str(error)}}
        (output / "result.json").write_text(json.dumps(failed, ensure_ascii=False, indent=2), encoding="utf-8")
        print(json.dumps(failed, ensure_ascii=False))
        raise SystemExit(1) from error
    print(json.dumps({"output": str((Path(__file__).resolve().parents[1] / args.output).resolve()), "jobId": payload["jobId"], "status": payload["status"], "processedCount": payload["processing"]["processedCount"], "observationCount": len(payload["observations"])}, ensure_ascii=False))


if __name__ == "__main__":
    main()
