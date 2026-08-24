"""Gemini-based advisory verification for annotated YOLO detections."""

from __future__ import annotations

import json
import os
import re
import time
from pathlib import Path
from typing import Any

from PIL import Image


ALLOWED_LABELS = {
    "crack", "mold", "peeling", "stain", "surface_defect", "water_damage",
    "hole", "tile_damage", "tile_crack", "pin_hole", "trowel_mark",
    "unknown_defect",
}

PROMPT = """You verify YOLO detections in a Korean rental-property inspection image.
Two versions of the same full image are supplied: first the clean original image,
then an annotated image with boxes labeled D1, D2, and so on. Use the annotated
image only to locate each candidate, and inspect the clean original for visual evidence.
The red boxes and text are overlays, not defects.

Missing a real defect is more harmful than keeping a false positive. Review each box only.
Ordinary objects, furniture, decorations, people, arrows, watermarks, labels,
reflections, seams, and clean surfaces are not defects.

Return JSON only:
{"detections":[{"candidateId":"D1","verdict":"defect|not_defect|uncertain",
"label":"allowed label","verdictConfidence":0.0,"reason":"short Korean explanation"}]}

Use not_defect only when the candidate is clearly an ordinary object, annotation,
pattern, reflection, seam, or clean surface. Faint stains, fine cracks, discoloration,
small holes, peeling edges, and ambiguous physical marks must be uncertain rather
than not_defect. verdictConfidence means certainty that the chosen verdict is correct;
for example, a very certain not_defect must have a high verdictConfidence.
Return exactly one item for every candidate ID. Allowed labels:
crack, mold, peeling, stain, surface_defect, water_damage, hole, tile_damage,
tile_crack, pin_hole, trowel_mark, unknown_defect.
"""


def _clean_json(text: str) -> dict[str, Any]:
    value = text.strip()
    if value.startswith("```"):
        lines = value.splitlines()[1:]
        if lines and lines[-1].strip() == "```":
            lines.pop()
        value = "\n".join(lines).strip()
    # Gemini can occasionally emit a literal \u that is not followed by four
    # hexadecimal digits inside a reason. Escape only those invalid sequences.
    value = re.sub(r"\\u(?![0-9a-fA-F]{4})", r"\\\\u", value)
    payload = json.loads(value)
    if not isinstance(payload, dict) or not isinstance(payload.get("detections"), list):
        raise ValueError("Gemini verifier response must contain a detections array")
    return payload


class GeminiDefectVerifier:
    """Verify all numbered boxes in one annotated image with one Gemini call."""

    def __init__(
        self,
        model: str = "gemini-3.5-flash-lite",
        retries: int = 3,
        min_request_interval_sec: float = 4.2,
    ) -> None:
        api_key = os.environ.get("GEMINI_API_KEY")
        if not api_key:
            raise RuntimeError("GEMINI_API_KEY environment variable is required")
        try:
            from google import genai
            from google.genai import types
        except ImportError as error:
            raise RuntimeError("google-genai is not installed; run pip install -r requirements.txt") from error
        self.client = genai.Client(api_key=api_key)
        self.types = types
        self.model = model
        self.retries = max(1, retries)
        self.min_request_interval_sec = max(0.0, min_request_interval_sec)
        self._last_request_started = 0.0

    def _wait_for_rate_limit(self) -> None:
        remaining = self.min_request_interval_sec - (time.monotonic() - self._last_request_started)
        if remaining > 0:
            time.sleep(remaining)
        self._last_request_started = time.monotonic()

    @staticmethod
    def _fallback(candidate: dict[str, Any], status: str, reason: str) -> dict[str, Any]:
        return {
            "provider": "gemini",
            "verdict": "uncertain",
            "label": candidate["label"],
            "confidence": 0.0,
            "reason": reason,
            "status": status,
        }

    def verify(
        self,
        original_path: Path,
        annotated_path: Path,
        candidates: list[dict[str, Any]],
    ) -> dict[str, Any]:
        metadata = [{
            "candidateId": item["candidateId"],
            "yoloLabel": item["label"],
        } for item in candidates]
        prompt = f"{PROMPT}\nCandidates: {json.dumps(metadata, ensure_ascii=False)}"
        last_error: Exception | None = None
        for attempt in range(self.retries):
            try:
                self._wait_for_rate_limit()
                with Image.open(original_path) as original_source, Image.open(annotated_path) as annotated_source:
                    original_image = original_source.convert("RGB")
                    annotated_image = annotated_source.convert("RGB")
                    response = self.client.models.generate_content(
                        model=self.model,
                        contents=[
                            prompt,
                            "Clean original image:",
                            original_image,
                            "Annotated candidate-location image:",
                            annotated_image,
                        ],
                        config=self.types.GenerateContentConfig(
                            response_mime_type="application/json",
                            temperature=0,
                        ),
                    )
                payload = _clean_json(response.text or "")
                raw_by_id = {
                    str(item.get("candidateId")): item
                    for item in payload["detections"] if isinstance(item, dict)
                }
                results: dict[str, dict[str, Any]] = {}
                for candidate in candidates:
                    candidate_id = candidate["candidateId"]
                    raw = raw_by_id.get(candidate_id)
                    if raw is None:
                        results[candidate_id] = self._fallback(
                            candidate, "incomplete", "Gemini 응답에서 해당 박스가 누락되어 유지합니다."
                        )
                        continue
                    verdict = str(raw.get("verdict", "uncertain")).lower()
                    if verdict not in {"defect", "not_defect", "uncertain"}:
                        verdict = "uncertain"
                    label = str(raw.get("label", candidate["label"])).lower()
                    if label not in ALLOWED_LABELS:
                        label = "unknown_defect"
                    try:
                        confidence = max(0.0, min(1.0, float(raw.get("verdictConfidence", 0.0))))
                    except (TypeError, ValueError):
                        confidence = 0.0
                    results[candidate_id] = {
                        "provider": "gemini",
                        "model": self.model,
                        "verdict": verdict,
                        "label": label,
                        "confidence": round(confidence, 6),
                        "reason": str(raw.get("reason", ""))[:300],
                        "status": "completed",
                    }
                return {"results": results, "error": None}
            except Exception as error:
                last_error = error
                if attempt + 1 < self.retries:
                    time.sleep(5 * (attempt + 1))
        reason = "Gemini 보조 검증 실패로 YOLO 결과를 유지합니다."
        return {
            "results": {
                item["candidateId"]: {
                    **self._fallback(item, "error", reason),
                    "model": self.model,
                    "error": {"type": type(last_error).__name__, "message": str(last_error)},
                }
                for item in candidates
            },
            "error": {"type": type(last_error).__name__, "message": str(last_error)},
        }
