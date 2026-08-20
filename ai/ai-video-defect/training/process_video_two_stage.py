"""Process a backend-provided video with the full-image two-stage YOLO pipeline."""

from __future__ import annotations

import argparse
import json
import time
from pathlib import Path
from typing import Any

import cv2
import numpy as np
import torch
from ultralytics import YOLO

from .predict_dacon_two_stage import box_iou


IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}


def display_path(path: Path, root: Path) -> str:
    try:
        return str(path.relative_to(root))
    except ValueError:
        return str(path)


def frame_quality(frame: np.ndarray, min_blur: float, min_brightness: float) -> tuple[bool, dict[str, float]]:
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    blur_score = float(cv2.Laplacian(gray, cv2.CV_64F).var())
    brightness = float(gray.mean())
    accepted = blur_score >= min_blur and brightness >= min_brightness
    return accepted, {"blur_score": round(blur_score, 3), "brightness": round(brightness, 3)}


def is_duplicate(frame: np.ndarray, previous_gray: np.ndarray | None, threshold: float) -> tuple[bool, np.ndarray]:
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    small = cv2.resize(gray, (32, 32), interpolation=cv2.INTER_AREA).astype(np.float32)
    if previous_gray is None:
        return False, small
    difference = float(np.mean(np.abs(small - previous_gray)))
    return difference <= threshold, small


def predict_frame(model: YOLO, frame: np.ndarray, imgsz: int, confidence: float, nms_iou: float, device: str) -> list[dict[str, Any]]:
    result = next(iter(model.predict(
        source=frame,
        imgsz=imgsz,
        conf=confidence,
        iou=nms_iou,
        max_det=300,
        device=device,
        verbose=False,
    )))
    items: list[dict[str, Any]] = []
    if result.boxes is None:
        return items
    names = result.names
    for box, cls_id, conf in zip(
        result.boxes.xyxy.cpu().tolist(),
        result.boxes.cls.cpu().tolist(),
        result.boxes.conf.cpu().tolist(),
    ):
        class_id = int(cls_id)
        items.append({
            "class_id": class_id,
            "class_name": names.get(class_id, str(class_id)),
            "confidence": round(float(conf), 6),
            "box_xyxy": [round(float(value), 2) for value in box],
        })
    return items


def merge_frame(binary: list[dict[str, Any]], multiclass: list[dict[str, Any]], threshold: float) -> list[dict[str, Any]]:
    merged = [dict(item, source="multiclass", review_status="needs_review") for item in multiclass]
    for item in binary:
        if not any(box_iou(item["box_xyxy"], other["box_xyxy"]) >= threshold for other in multiclass):
            merged.append({
                "class_id": None,
                "class_name": "unknown_defect",
                "confidence": item["confidence"],
                "box_xyxy": item["box_xyxy"],
                "source": "binary_only",
                "review_status": "needs_review",
            })
    return sorted(merged, key=lambda item: item["confidence"], reverse=True)


def backend_detection(item: dict[str, Any]) -> dict[str, Any]:
    left, top, right, bottom = [int(round(value)) for value in item["box_xyxy"]]
    classified = item["class_name"] != "unknown_defect"
    return {
        "classId": item["class_id"],
        "label": item["class_name"],
        "confidence": item["confidence"],
        "box": {"left": left, "top": top, "right": right, "bottom": bottom},
        "classificationStatus": "classified" if classified else "unclassified",
        "reviewStatus": item.get("review_status", "needs_review"),
    }


def same_observation(a: dict[str, Any], b: dict[str, Any], threshold: float) -> bool:
    label_a = a.get("label", a.get("class_name"))
    label_b = b.get("label", b.get("class_name"))
    if label_a != label_b:
        return False
    return box_iou(a["box_xyxy"], b["box_xyxy"]) >= threshold


def group_observations(frame_results: list[dict[str, Any]], threshold: float) -> list[dict[str, Any]]:
    groups: list[dict[str, Any]] = []
    for frame_result in frame_results:
        for detection in frame_result["merged_detections"]:
            match = next((group for group in groups if same_observation(group["last_detection"], detection, threshold)), None)
            if match is None:
                groups.append({
                    "detections": [detection],
                    "frames": [frame_result],
                    "last_detection": detection,
                })
            else:
                match["detections"].append(detection)
                match["frames"].append(frame_result)
                match["last_detection"] = detection
    observations = []
    for index, group in enumerate(groups, start=1):
        best = max(group["detections"], key=lambda item: item["confidence"])
        best_frame = max(group["frames"], key=lambda item: max(
            (d["confidence"] for d in item["merged_detections"] if same_observation(best, d, threshold)),
            default=0.0,
        ))
        result = backend_detection(best)
        result.update({
            "observationGroupId": f"obs-{index:04d}",
            "representativeFrameId": best_frame["frame_id"],
            "timestampSec": best_frame["timestamp_sec"],
            "frameCount": len({frame["frame_id"] for frame in group["frames"]}),
            "evidencePath": best_frame["evidence_path"],
        })
        observations.append(result)
    return observations


def save_observation_crops(
    observations: list[dict[str, Any]], output: Path, root: Path, margin_ratio: float
) -> None:
    margin_ratio = max(0.0, margin_ratio)
    crops_dir = output / "crops"
    crops_dir.mkdir(exist_ok=True)
    for observation in observations:
        observation["cropPath"] = None
        evidence_value = observation.get("evidencePath")
        if not evidence_value:
            continue
        evidence_path = Path(evidence_value)
        if not evidence_path.is_absolute():
            evidence_path = root / evidence_path
        frame = cv2.imread(str(evidence_path))
        if frame is None:
            continue

        image_height, image_width = frame.shape[:2]
        box = observation["box"]
        box_width = max(1, box["right"] - box["left"])
        box_height = max(1, box["bottom"] - box["top"])
        margin_x = int(round(box_width * margin_ratio))
        margin_y = int(round(box_height * margin_ratio))
        left = max(0, box["left"] - margin_x)
        top = max(0, box["top"] - margin_y)
        right = min(image_width, box["right"] + margin_x)
        bottom = min(image_height, box["bottom"] + margin_y)
        if right <= left or bottom <= top:
            continue

        crop = frame[top:bottom, left:right]
        safe_label = "".join(character if character.isalnum() or character in {"-", "_"} else "_" for character in observation["label"])
        crop_path = crops_dir / f"{observation['observationGroupId']}_{safe_label}.jpg"
        if cv2.imwrite(str(crop_path), crop):
            observation["cropPath"] = display_path(crop_path, root)
            observation["cropImage"] = {"width": right - left, "height": bottom - top}


def process_video(args: argparse.Namespace) -> dict[str, Any]:
    root = Path(__file__).resolve().parents[1]
    video_path = (root / args.video).resolve()
    output = (root / args.output).resolve()
    output.mkdir(parents=True, exist_ok=True)
    evidence_dir = output / "evidence"
    evidence_dir.mkdir(exist_ok=True)
    if not video_path.is_file():
        raise FileNotFoundError(f"Video not found: {video_path}")

    capture = cv2.VideoCapture(str(video_path))
    if not capture.isOpened():
        raise RuntimeError(f"Could not open video: {video_path}")
    fps = float(capture.get(cv2.CAP_PROP_FPS) or 0.0)
    width = int(capture.get(cv2.CAP_PROP_FRAME_WIDTH) or 0)
    height = int(capture.get(cv2.CAP_PROP_FRAME_HEIGHT) or 0)
    frame_count = int(capture.get(cv2.CAP_PROP_FRAME_COUNT) or 0)
    if fps <= 0 or width <= 0 or height <= 0:
        capture.release()
        raise RuntimeError("Video metadata is invalid: FPS, width, or height is zero")

    device = "0" if torch.cuda.is_available() else "cpu"
    binary_model = YOLO(str((root / args.binary).resolve()))
    multiclass_model = YOLO(str((root / args.multiclass).resolve()))
    interval_frames = max(1, round(fps * args.interval_sec))
    frame_results: list[dict[str, Any]] = []
    previous_gray: np.ndarray | None = None
    frame_index = 0
    sampled_count = 0
    filtered_count = 0
    started = time.perf_counter()

    while True:
        ok, frame = capture.read()
        if not ok:
            break
        if frame_index % interval_frames != 0:
            frame_index += 1
            continue
        sampled_count += 1
        accepted, quality = frame_quality(frame, args.min_blur, args.min_brightness)
        duplicate, current_gray = is_duplicate(frame, previous_gray, args.duplicate_threshold)
        previous_gray = current_gray
        if not accepted or duplicate:
            filtered_count += 1
            frame_index += 1
            continue

        timestamp_sec = round(frame_index / fps, 3)
        multiclass = predict_frame(multiclass_model, frame, args.multiclass_imgsz, args.confidence, args.nms_iou, device)
        binary = predict_frame(binary_model, frame, args.binary_imgsz, args.confidence, args.nms_iou, device)
        merged = merge_frame(binary, multiclass, args.merge_iou)
        evidence_path = None
        if merged:
            evidence_name = f"frame_{frame_index:08d}_{timestamp_sec:.3f}s.jpg"
            evidence_path = evidence_dir / evidence_name
            cv2.imwrite(str(evidence_path), frame)
        frame_results.append({
            "frame_id": frame_index,
            "timestamp_sec": timestamp_sec,
            "quality": quality,
            "evidence_path": display_path(evidence_path, root) if evidence_path else None,
            "binary_detections": binary,
            "multiclass_detections": multiclass,
            "merged_detections": merged,
        })
        frame_index += 1
    capture.release()

    observations = group_observations(frame_results, args.group_iou)
    for observation in observations:
        if observation["frameCount"] == 1:
            observation["reviewStatus"] = "needs_review"
    save_observation_crops(observations, output, root, args.crop_margin_ratio)
    elapsed = time.perf_counter() - started
    payload = {
        "job_id": args.job_id,
        "status": "completed",
        "video": {"path": display_path(video_path, root), "width": width, "height": height, "fps": fps, "frame_count": frame_count},
        "processing": {"interval_sec": args.interval_sec, "sampled_count": sampled_count, "filtered_count": filtered_count, "processed_count": len(frame_results), "device": device, "elapsed_sec": round(elapsed, 3)},
        "observations": observations,
        "frame_results": [
            {"frame_id": row["frame_id"], "timestamp_sec": row["timestamp_sec"], "image": {"width": width, "height": height}, "detections": [backend_detection(item) for item in row["merged_detections"]], "evidence_path": row["evidence_path"]}
            for row in frame_results
        ],
    }
    (output / "result.json").write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    return payload


def main() -> None:
    parser = argparse.ArgumentParser(description="Run video frame processing and full-image two-stage YOLO inference.")
    parser.add_argument("--video", type=Path, required=True)
    parser.add_argument("--job-id", default="local-video-test")
    parser.add_argument("--output", type=Path, default=Path("reports/video_two_stage_test"))
    parser.add_argument("--binary", type=Path, default=Path("models/active/two_stage_negative_rot4/binary/best.pt"))
    parser.add_argument("--multiclass", type=Path, default=Path("models/active/two_stage_negative_rot4/multiclass/best.pt"))
    parser.add_argument("--interval-sec", type=float, default=1.0)
    parser.add_argument("--min-blur", type=float, default=50.0)
    parser.add_argument("--min-brightness", type=float, default=25.0)
    parser.add_argument("--duplicate-threshold", type=float, default=3.0)
    parser.add_argument("--confidence", type=float, default=0.0325)
    parser.add_argument("--nms-iou", type=float, default=0.40)
    parser.add_argument("--merge-iou", type=float, default=0.30)
    parser.add_argument("--group-iou", type=float, default=0.30)
    parser.add_argument("--crop-margin-ratio", type=float, default=0.15)
    parser.add_argument("--multiclass-imgsz", type=int, default=320)
    parser.add_argument("--binary-imgsz", type=int, default=640)
    args = parser.parse_args()
    try:
        payload = process_video(args)
    except Exception as error:
        root = Path(__file__).resolve().parents[1]
        output = (root / args.output).resolve()
        output.mkdir(parents=True, exist_ok=True)
        failed = {
            "job_id": args.job_id,
            "status": "failed",
            "error": {"type": type(error).__name__, "message": str(error)},
        }
        (output / "result.json").write_text(json.dumps(failed, ensure_ascii=False, indent=2), encoding="utf-8")
        print(json.dumps(failed, ensure_ascii=False))
        raise SystemExit(1) from error
    print(json.dumps({"output": str((Path(__file__).resolve().parents[1] / args.output).resolve()), "job_id": payload["job_id"], "status": payload["status"], "processed_count": payload["processing"]["processed_count"], "observation_count": len(payload["observations"])}, ensure_ascii=False))


if __name__ == "__main__":
    main()
