"""Run the full-image Binary YOLO + multiclass YOLO pipeline on unlabeled images."""

from __future__ import annotations

import argparse
import json
import time
from collections import Counter
from pathlib import Path

import torch
from ultralytics import YOLO


def box_iou(a: list[float], b: list[float]) -> float:
    left, top = max(a[0], b[0]), max(a[1], b[1])
    right, bottom = min(a[2], b[2]), min(a[3], b[3])
    intersection = max(0.0, right - left) * max(0.0, bottom - top)
    area_a = max(0.0, a[2] - a[0]) * max(0.0, a[3] - a[1])
    area_b = max(0.0, b[2] - b[0]) * max(0.0, b[3] - b[1])
    union = area_a + area_b - intersection
    return intersection / union if union else 0.0


def predict_model(model: YOLO, source: Path, imgsz: int, confidence: float, iou: float, device: str):
    started = time.perf_counter()
    predictions: dict[str, list[dict]] = {}
    results = model.predict(
        source=str(source), imgsz=imgsz, conf=confidence, iou=iou,
        max_det=300, device=device, stream=True, verbose=False,
    )
    for result in results:
        items = []
        if result.boxes is not None:
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
        predictions[Path(result.path).name] = items
    return predictions, time.perf_counter() - started


def merge(binary: dict[str, list[dict]], multiclass: dict[str, list[dict]], threshold: float):
    merged: dict[str, list[dict]] = {}
    for filename in sorted(binary.keys() | multiclass.keys()):
        items = [dict(item, source="multiclass", review_status="model_classified") for item in multiclass.get(filename, [])]
        for item in binary.get(filename, []):
            if not any(box_iou(item["box_xyxy"], other["box_xyxy"]) >= threshold for other in multiclass.get(filename, [])):
                items.append({
                    "class_id": None,
                    "class_name": "unknown_defect",
                    "confidence": item["confidence"],
                    "box_xyxy": item["box_xyxy"],
                    "source": "binary_only",
                    "review_status": "needs_review",
                })
        merged[filename] = items
    return merged


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", type=Path, default=Path("Data/DACON/open/test"))
    parser.add_argument("--multiclass", type=Path, default=Path("models/active/two_stage_negative_rot4/multiclass/best.pt"))
    parser.add_argument("--binary", type=Path, default=Path("models/active/two_stage_negative_rot4/binary/best.pt"))
    parser.add_argument("--output", type=Path, default=Path("reports/2026-08-19_dacon_open_two_stage_test"))
    parser.add_argument("--multiclass-imgsz", type=int, default=320)
    parser.add_argument("--binary-imgsz", type=int, default=640)
    parser.add_argument("--confidence", type=float, default=0.0325)
    parser.add_argument("--nms-iou", type=float, default=0.40)
    parser.add_argument("--merge-iou", type=float, default=0.30)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    source = (root / args.input).resolve()
    output = (root / args.output).resolve()
    multiclass_path = (root / args.multiclass).resolve()
    binary_path = (root / args.binary).resolve()
    device = "0" if torch.cuda.is_available() else "cpu"
    files = sorted(p for p in source.iterdir() if p.is_file() and p.suffix.lower() in {".jpg", ".jpeg", ".png", ".bmp", ".webp"})
    if not files:
        raise SystemExit(f"No images found: {source}")

    multiclass, multiclass_seconds = predict_model(YOLO(str(multiclass_path)), source, args.multiclass_imgsz, args.confidence, args.nms_iou, device)
    binary, binary_seconds = predict_model(YOLO(str(binary_path)), source, args.binary_imgsz, args.confidence, args.nms_iou, device)
    merged = merge(binary, multiclass, args.merge_iou)
    rows = []
    class_counts = Counter()
    unknown_count = 0
    for filename in sorted(set(files_by_name.name for files_by_name in files)):
        multi_items = multiclass.get(filename, [])
        binary_items = binary.get(filename, [])
        merged_items = merged.get(filename, [])
        unknown_count += sum(item["class_name"] == "unknown_defect" for item in merged_items)
        class_counts.update(item["class_name"] for item in merged_items)
        rows.append({
            "filename": filename,
            "multiclass_detections": multi_items,
            "binary_detections": binary_items,
            "merged_detections": merged_items,
            "multiclass_count": len(multi_items),
            "binary_count": len(binary_items),
            "merged_count": len(merged_items),
            "unknown_defect_count": sum(item["class_name"] == "unknown_defect" for item in merged_items),
        })
    payload = {
        "task": "temporary_unlabeled_two_stage_defect_test",
        "input": str(source.relative_to(root)),
        "image_count": len(rows),
        "device": device,
        "models": {"binary": str(binary_path.relative_to(root)), "multiclass": str(multiclass_path.relative_to(root))},
        "conditions": {"confidence": args.confidence, "nms_iou": args.nms_iou, "merge_iou": args.merge_iou, "multiclass_imgsz": args.multiclass_imgsz, "binary_imgsz": args.binary_imgsz},
        "timing_seconds": {"multiclass": round(multiclass_seconds, 3), "binary": round(binary_seconds, 3), "total": round(multiclass_seconds + binary_seconds, 3), "per_image_ms": round((multiclass_seconds + binary_seconds) / len(rows) * 1000, 3)},
        "summary": {"merged_detections_by_class": dict(class_counts), "unknown_defect_count": unknown_count, "images_with_any_detection": sum(bool(row["merged_detections"]) for row in rows)},
        "predictions": rows,
    }
    output.mkdir(parents=True, exist_ok=True)
    (output / "predictions.json").write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    (output / "summary.md").write_text(
        f"# DACON open 임시 2단계 하자 탐지 테스트\n\n"
        f"- 입력: `{source.relative_to(root)}`\n- 이미지 수: {len(rows)}장\n- 장치: `{device}`\n"
        f"- 다중 클래스 추론: {multiclass_seconds:.3f}초\n- Binary 추론: {binary_seconds:.3f}초\n"
        f"- 전체 처리 시간: {multiclass_seconds + binary_seconds:.3f}초 ({(multiclass_seconds + binary_seconds) / len(rows) * 1000:.3f}ms/장)\n\n"
        f"## 결과 요약\n\n- 탐지 결과가 있는 이미지: {sum(bool(row['merged_detections']) for row in rows)}장\n- `unknown_defect` 후보: {unknown_count}개\n- 클래스별 병합 탐지 수: `{dict(class_counts)}`\n\n"
        "## 해석 범위\n\n"
        "이 입력에는 탐지용 bounding box 정답 라벨이 없으므로 Recall, 미탐률, 오탐률, mAP를 계산하지 않았습니다. "
        "또한 DACON open 이미지는 현재 방 전체 촬영본이 아닌 결함 분류용 이미지일 수 있어, 이번 결과는 모델 실행 여부와 예측 분포를 확인하는 임시 smoke/domain check로만 사용합니다.\n",
        encoding="utf-8",
    )
    print(json.dumps({"output": str(output), "image_count": len(rows), "device": device, "unknown_defect_count": unknown_count, "total_seconds": round(multiclass_seconds + binary_seconds, 3)}, ensure_ascii=False))


if __name__ == "__main__":
    main()
