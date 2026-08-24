"""Re-render existing box-filter results with current label styling."""

from __future__ import annotations

import argparse
import json
from pathlib import Path

import cv2

from training.process_images_two_stage import draw_detections


def candidate_number(item: dict) -> int:
    value = str(item.get("candidateId", "D0"))
    try:
        return int(value.removeprefix("D"))
    except ValueError:
        return 0


def main() -> None:
    parser = argparse.ArgumentParser(description="Re-render candidate and final box images")
    parser.add_argument("--result", type=Path, required=True)
    parser.add_argument("--input", type=Path, required=True)
    args = parser.parse_args()
    result_path = args.result.resolve()
    input_dir = args.input.resolve()
    payload = json.loads(result_path.read_text(encoding="utf-8"))
    candidate_dir = result_path.parent / "annotated" / "candidates"
    final_dir = result_path.parent / "annotated" / "final"
    for item in payload["images"]:
        source_path = input_dir / item["filename"]
        image = cv2.imread(str(source_path))
        if image is None:
            raise ValueError(f"Unable to read image: {source_path}")
        candidates = sorted(
            item["detections"] + item.get("rejectedDetections", []),
            key=candidate_number,
        )
        draw_detections(image, candidates, candidate_dir / item["filename"], show_candidate_id=True)
        draw_detections(image, item["detections"], final_dir / item["filename"], show_candidate_id=False)
    print(json.dumps({"rendered": len(payload["images"]), "output": str(result_path.parent)}, ensure_ascii=False))


if __name__ == "__main__":
    main()
