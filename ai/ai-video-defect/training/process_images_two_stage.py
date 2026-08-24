"""Process server-provided images with full-image Binary + multiclass YOLO."""

from __future__ import annotations

import argparse
import json
import os
import time
from pathlib import Path

import cv2
import torch
from PIL import Image, ImageDraw, ImageFont
from ultralytics import YOLO

from training.predict_dacon_two_stage import box_iou


IMAGE_SUFFIXES = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}

LABEL_DISPLAY = {
    "defect": "하자 의심",
    "unknown_defect": "하자 의심",
    "crack": "균열",
    "mold": "곰팡이",
    "peeling": "들뜸·박리",
    "water_damage": "누수",
    "tile_damage": "타일 손상",
    "hole": "구멍",
    "tile_crack": "타일 균열",
    "paint_drips": "페인트 흘러내림",
    "pin_hole": "미세 구멍",
    "surface_defect": "표면 하자",
    "stain": "오염",
    "trowel_mark": "마감 자국",
}

LABEL_COLORS_RGB = {
    "crack": (229, 57, 53),
    "mold": (46, 125, 50),
    "peeling": (251, 140, 0),
    "water_damage": (30, 136, 229),
    "tile_damage": (142, 36, 170),
    "hole": (109, 76, 65),
    "tile_crack": (216, 27, 96),
    "paint_drips": (94, 53, 177),
    "pin_hole": (0, 137, 123),
    "surface_defect": (249, 168, 37),
    "stain": (85, 139, 47),
    "trowel_mark": (84, 110, 122),
    "defect": (244, 81, 30),
    "unknown_defect": (244, 81, 30),
}


def display_label(label: str) -> str:
    return LABEL_DISPLAY.get(label, label)


def display_color(label: str) -> tuple[int, int, int]:
    return LABEL_COLORS_RGB.get(label, (244, 81, 30))


def display_color_hex(label: str) -> str:
    return "#{:02X}{:02X}{:02X}".format(*display_color(label))


def load_korean_font(size: int) -> ImageFont.FreeTypeFont:
    configured = os.environ.get("DEFECT_LABEL_FONT")
    candidates = [
        configured,
        "C:/Windows/Fonts/malgunbd.ttf",
        "C:/Windows/Fonts/malgun.ttf",
        "/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc",
        "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
        "/usr/share/fonts/truetype/nanum/NanumGothicBold.ttf",
        "/System/Library/Fonts/AppleSDGothicNeo.ttc",
    ]
    for candidate in candidates:
        if candidate and Path(candidate).is_file():
            return ImageFont.truetype(str(candidate), size=size)
    raise RuntimeError(
        "Korean label font not found. Install fonts-noto-cjk or set DEFECT_LABEL_FONT."
    )


def display_path(path: Path, root: Path) -> str:
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
            "displayLabel": display_label(names.get(class_id, str(class_id))),
            "displayColor": display_color_hex(names.get(class_id, str(class_id))),
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
                "classId": None,
                "label": "unknown_defect",
                "displayLabel": display_label("unknown_defect"),
                "displayColor": display_color_hex("unknown_defect"),
                "confidence": binary_item["confidence"],
                "box": binary_item["box"],
                "classificationStatus": "unclassified",
                "reviewStatus": "needs_review",
                "defectConfidence": binary_item["confidence"],
                "classConfidence": None,
                "source": "binary_only",
            })
    return sorted(merged, key=lambda item: item["confidence"], reverse=True)


def draw_detections(
    image,
    detections: list[dict],
    destination: Path,
    show_candidate_id: bool = False,
) -> None:
    rendered = Image.fromarray(cv2.cvtColor(image, cv2.COLOR_BGR2RGB))
    drawer = ImageDraw.Draw(rendered)
    font_size = max(18, min(34, rendered.width // 45))
    font = load_korean_font(font_size)
    line_width = max(3, rendered.width // 500)
    for detection in detections:
        box = detection["box"]
        left, top = int(box["left"]), int(box["top"])
        right, bottom = int(box["right"]), int(box["bottom"])
        label_key = detection["label"]
        color = display_color(label_key)
        candidate = f"{detection.get('candidateId')} " if show_candidate_id else ""
        label = f"{candidate}{display_label(label_key)} {detection['confidence']:.0%}"
        drawer.rectangle((left, top, right, bottom), outline=color, width=line_width)
        text_box = drawer.textbbox((0, 0), label, font=font)
        text_width = text_box[2] - text_box[0]
        text_height = text_box[3] - text_box[1]
        text_left = max(0, min(left, rendered.width - text_width - 10))
        text_top = top - text_height - 10
        if text_top < 0:
            text_top = min(rendered.height - text_height - 8, top + 4)
        drawer.rounded_rectangle(
            (text_left, text_top, text_left + text_width + 10, text_top + text_height + 8),
            radius=4,
            fill=color,
        )
        drawer.text((text_left + 5, text_top + 2), label, font=font, fill=(255, 255, 255))
    destination.parent.mkdir(parents=True, exist_ok=True)
    rendered.save(destination, quality=92)


def process_images(args: argparse.Namespace) -> dict:
    root = Path(__file__).resolve().parents[1]
    input_path = (root / args.input).resolve()
    output = (root / args.output).resolve()
    binary_path = (root / args.binary).resolve()
    multiclass_path = (root / args.multiclass).resolve()
    if input_path.is_file() and input_path.suffix.lower() in IMAGE_SUFFIXES:
        image_paths = [input_path]
    elif input_path.is_dir():
        image_paths = sorted(path for path in input_path.iterdir() if path.is_file() and path.suffix.lower() in IMAGE_SUFFIXES)
    else:
        raise FileNotFoundError(f"Image file or directory not found: {input_path}")
    if not image_paths:
        raise ValueError(f"No supported images found: {input_path}")

    device = "0" if torch.cuda.is_available() else "cpu"
    binary_model = YOLO(str(binary_path))
    multiclass_model = YOLO(str(multiclass_path))
    image_results = []
    observations = []
    started = time.perf_counter()
    annotated_candidates_dir = output / "annotated" / "candidates"
    annotated_final_dir = output / "annotated" / "final"
    verifier = None
    if args.defect_verifier == "gemini":
        from training.gemini_defect_verifier import GeminiDefectVerifier
        verifier = GeminiDefectVerifier(args.gemini_defect_model, args.gemini_defect_retries)
    verification_calls = 0
    verification_errors = 0

    for image_index, image_path in enumerate(image_paths, start=1):
        image = cv2.imread(str(image_path))
        if image is None:
            raise ValueError(f"Unable to read image: {image_path}")
        height, width = image.shape[:2]
        multiclass = predict_image(multiclass_model, image_path, args.multiclass_imgsz, args.confidence, args.nms_iou, device)
        binary = predict_image(binary_model, image_path, args.binary_imgsz, args.confidence, args.nms_iou, device)
        merged = merge_detections(binary, multiclass, args.merge_iou)
        for detection_index, detection in enumerate(merged, start=1):
            detection["candidateId"] = f"D{detection_index}"
        image_id = f"image-{image_index:04d}"
        candidate_annotated_path = None
        final_annotated_path = None
        rejected = []
        if merged:
            candidate_annotated_path = annotated_candidates_dir / image_path.name
            draw_detections(image, merged, candidate_annotated_path, show_candidate_id=True)
            if verifier is not None:
                verification_calls += 1
                verification = verifier.verify(image_path, candidate_annotated_path, merged)
                if verification["error"] is not None:
                    verification_errors += 1
                for detection in merged:
                    detection["verification"] = verification["results"][detection["candidateId"]]
                    should_reject = (
                        detection["verification"]["verdict"] == "not_defect"
                        and detection["verification"]["confidence"] >= args.gemini_reject_confidence
                    )
                    detection["verification"]["filterDecision"] = "rejected" if should_reject else "kept"
                rejected = [item for item in merged if item["verification"]["filterDecision"] == "rejected"]
                merged = [item for item in merged if item["verification"]["filterDecision"] == "kept"]
            final_annotated_path = annotated_final_dir / image_path.name
            draw_detections(image, merged, final_annotated_path, show_candidate_id=False)

        for detection_index, detection in enumerate(merged, start=1):
            observation_id = f"obs-{image_index:04d}-{detection_index:03d}"
            observation = dict(detection)
            observation.update({
                "observationGroupId": observation_id,
                "imageId": image_id,
                "filename": image_path.name,
                "representativeImageId": image_id,
                "evidencePath": display_path(image_path, root),
                "annotatedPath": display_path(final_annotated_path, root) if final_annotated_path else None,
            })
            observations.append(observation)

        image_results.append({
            "imageId": image_id,
            "filename": image_path.name,
            "image": {"width": width, "height": height},
            "detections": merged,
            "binaryDetections": binary,
            "multiclassDetections": multiclass,
            "rejectedDetections": rejected,
            "evidencePath": display_path(image_path, root),
            "candidateAnnotatedPath": display_path(candidate_annotated_path, root) if candidate_annotated_path else None,
            "annotatedPath": display_path(final_annotated_path, root) if final_annotated_path else None,
        })

    elapsed = time.perf_counter() - started
    payload = {
        "jobId": args.job_id,
        "status": "completed",
        "input": {"path": display_path(input_path, root), "imageCount": len(image_paths)},
            "processing": {
            "mode": "image_batch",
            "processedCount": len(image_results),
            "device": device,
            "elapsedSec": round(elapsed, 3),
            "confidence": args.confidence,
            "mergeIou": args.merge_iou,
            "defectVerifier": args.defect_verifier,
            "geminiDefectModel": args.gemini_defect_model if args.defect_verifier == "gemini" else None,
            "geminiRejectConfidence": args.gemini_reject_confidence if args.defect_verifier == "gemini" else None,
            "verificationCalls": verification_calls,
            "verificationErrors": verification_errors,
        },
        "images": image_results,
        "observations": observations,
    }
    output.mkdir(parents=True, exist_ok=True)
    (output / "result.json").write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    return payload


def main() -> None:
    parser = argparse.ArgumentParser(description="Run full-image Binary + multiclass YOLO on server-provided images.")
    parser.add_argument("--input", type=Path, required=True, help="An image file or a directory of images")
    parser.add_argument("--job-id", default="local-image-test")
    parser.add_argument("--output", type=Path, default=Path("reports/image_two_stage_test"))
    parser.add_argument("--binary", type=Path, default=Path("models/active/two_stage_negative_rot4/binary/best.pt"))
    parser.add_argument("--multiclass", type=Path, default=Path("models/active/two_stage_negative_rot4/multiclass/best.pt"))
    parser.add_argument("--multiclass-imgsz", type=int, default=320)
    parser.add_argument("--binary-imgsz", type=int, default=640)
    parser.add_argument("--confidence", type=float, default=0.0325)
    parser.add_argument("--nms-iou", type=float, default=0.40)
    parser.add_argument("--merge-iou", type=float, default=0.30)
    parser.add_argument("--defect-verifier", choices=("disabled", "gemini"), default="disabled")
    parser.add_argument("--gemini-defect-model", default="gemini-3.5-flash-lite")
    parser.add_argument("--gemini-defect-retries", type=int, default=3)
    parser.add_argument("--gemini-reject-confidence", type=float, default=0.90)
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
