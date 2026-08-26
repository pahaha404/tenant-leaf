"""Gemini-based room classification and sequence smoothing for sampled images."""

from __future__ import annotations

import json
import os
import time
from collections import Counter
from pathlib import Path
from typing import Any

from PIL import Image


ROOM_LABELS = ("bathroom", "kitchen", "living_room", "unknown")

ROOM_PROMPT = """You classify an ordered batch of sampled images from a Korean studio-apartment inspection.
For every supplied image, choose exactly one room label:
- bathroom: toilet, shower, bathtub, washbasin, bathroom tiles
- kitchen: sink, cooktop, kitchen cabinets, refrigerator or cooking area
- living_room: main living/sleeping room, bedroom-like open room or general indoor room
- unknown: doorway transition, close-up wall/object, blurred frame, ambiguous or another space

Images are supplied in sequence_id order. Return one item for every sequence_id using it as frameId.
Return JSON only in this shape:
{"frames":[{"frameId":123,"room":"bathroom","uncertain":false,"containsPerson":false}]}
Do not invent another label. Set uncertain=true whenever the visual evidence is weak.
Set containsPerson=true when any person, face, or recognizable human body part appears in the image.
"""


def _clean_json_text(text: str) -> str:
    value = text.strip()
    if value.startswith("```"):
        lines = value.splitlines()
        if lines and lines[0].startswith("```"):
            lines = lines[1:]
        if lines and lines[-1].strip() == "```":
            lines = lines[:-1]
        value = "\n".join(lines).strip()
    return value


def parse_room_response(text: str, expected_frame_ids: list[int]) -> list[dict[str, Any]]:
    """Validate a Gemini JSON response and preserve the requested frame order."""
    payload = json.loads(_clean_json_text(text))
    items = payload.get("frames") if isinstance(payload, dict) else None
    if not isinstance(items, list):
        raise ValueError("Gemini response must contain a frames array")

    parsed: dict[int, dict[str, Any]] = {}
    for item in items:
        if not isinstance(item, dict):
            continue
        raw_id = item.get("frameId", item.get("frame_id"))
        try:
            frame_id = int(raw_id)
        except (TypeError, ValueError):
            continue
        room = str(item.get("room", "unknown")).strip().lower()
        if room not in ROOM_LABELS:
            room = "unknown"
        raw_contains_person = item.get("containsPerson")
        parsed[frame_id] = {
            "frameId": frame_id,
            "room": room,
            "uncertain": bool(item.get("uncertain", room == "unknown")),
            # Missing privacy metadata is treated conservatively so an older or
            # malformed provider response cannot become a representative photo.
            "containsPerson": (
                raw_contains_person if isinstance(raw_contains_person, bool) else True
            ),
        }

    missing = [frame_id for frame_id in expected_frame_ids if frame_id not in parsed]
    if missing:
        raise ValueError(f"Gemini response omitted frame ids: {missing}")
    return [parsed[frame_id] for frame_id in expected_frame_ids]


def stabilize_room_predictions(
    predictions: list[dict[str, Any]], window_size: int = 5, min_votes: int = 3
) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    """Apply centered majority voting and build contiguous room segments."""
    if window_size < 1 or window_size % 2 == 0:
        raise ValueError("window_size must be a positive odd number")
    if min_votes < 1:
        raise ValueError("min_votes must be positive")
    if not predictions:
        return [], []

    radius = window_size // 2
    stabilized: list[dict[str, Any]] = []
    for index, prediction in enumerate(predictions):
        start = max(0, index - radius)
        end = min(len(predictions), index + radius + 1)
        labels = [
            row["room"] for row in predictions[start:end]
            if row["room"] != "unknown" and not row.get("uncertain", False)
        ]
        counts = Counter(labels)
        best_label, best_count = counts.most_common(1)[0] if counts else ("unknown", 0)
        available_majority = len(predictions[start:end]) // 2 + 1
        required_votes = min(min_votes, available_majority)
        stable_room = best_label if best_count >= required_votes else "unknown"
        stabilized.append({**prediction, "stableRoom": stable_room})

    segments: list[dict[str, Any]] = []
    for row in stabilized:
        room = row["stableRoom"]
        if not segments or segments[-1]["room"] != room:
            segment_id = f"room-seg-{len(segments) + 1:04d}"
            segments.append({
                "roomSegmentId": segment_id,
                "room": room,
                "startFrameId": row["frameId"],
                "endFrameId": row["frameId"],
                "startTimestampSec": row["timestampSec"],
                "endTimestampSec": row["timestampSec"],
                "frameCount": 1,
            })
        else:
            segment = segments[-1]
            segment["endFrameId"] = row["frameId"]
            segment["endTimestampSec"] = row["timestampSec"]
            segment["frameCount"] += 1
        row["roomSegmentId"] = segments[-1]["roomSegmentId"]
    return stabilized, segments


class GeminiRoomClassifier:
    """Classify sampled frame images through the Gemini Developer API."""

    def __init__(
        self,
        model: str = "gemini-3.5-flash-lite",
        batch_size: int = 10,
        max_image_size: int = 384,
        retries: int = 2,
    ) -> None:
        api_key = os.environ.get("GEMINI_API_KEY")
        if not api_key:
            raise RuntimeError("GEMINI_API_KEY environment variable is required")
        try:
            from google import genai
            from google.genai import types
        except ImportError as error:
            raise RuntimeError("google-genai is not installed; run pip install -r requirements.txt") from error
        request_timeout_ms = max(1_000, int(os.environ.get("GEMINI_REQUEST_TIMEOUT_MS", "15000")))
        self.client = genai.Client(
            api_key=api_key,
            http_options=types.HttpOptions(timeout=request_timeout_ms),
        )
        self.types = types
        self.model = model
        self.batch_size = max(1, batch_size)
        self.max_image_size = max(64, max_image_size)
        self.retries = max(1, retries)

    def _open_image(self, path: Path) -> Image.Image:
        with Image.open(path) as source:
            image = source.convert("RGB")
        image.thumbnail((self.max_image_size, self.max_image_size), Image.Resampling.LANCZOS)
        return image

    def _classify_batch(self, rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
        images: list[Image.Image] = []
        contents: list[Any] = [ROOM_PROMPT]
        for row in rows:
            contents.append(
                f"sequence_id={row['frameId']}, "
                f"timestamp_sec={row.get('timestampSec')}, filename={row['filename']}"
            )
            image = self._open_image(Path(row["imagePath"]))
            images.append(image)
            contents.append(image)
        try:
            response = self.client.models.generate_content(
                model=self.model,
                contents=contents,
                config=self.types.GenerateContentConfig(
                    response_mime_type="application/json",
                    temperature=0,
                ),
            )
            return parse_room_response(response.text or "", [row["frameId"] for row in rows])
        finally:
            for image in images:
                image.close()

    def classify(self, rows: list[dict[str, Any]]) -> dict[str, Any]:
        results: list[dict[str, Any]] = []
        errors: list[dict[str, Any]] = []
        api_calls = 0
        started = time.perf_counter()
        for offset in range(0, len(rows), self.batch_size):
            batch = rows[offset:offset + self.batch_size]
            last_error: Exception | None = None
            for attempt in range(self.retries):
                api_calls += 1
                try:
                    classified = self._classify_batch(batch)
                    by_id = {item["frameId"]: item for item in classified}
                    for row in batch:
                        result = by_id[row["frameId"]]
                        results.append({
                            **result,
                            "timestampSec": row["timestampSec"],
                            "filename": row["filename"],
                            "provider": "gemini",
                            "model": self.model,
                        })
                    last_error = None
                    break
                except Exception as error:  # SDK exposes several provider-specific exception classes.
                    last_error = error
                    if attempt + 1 < self.retries:
                        time.sleep(5 * (2 ** attempt))
            if last_error is not None:
                errors.append({
                    "frameIds": [row["frameId"] for row in batch],
                    "type": type(last_error).__name__,
                    "message": str(last_error),
                })
                for row in batch:
                    results.append({
                        "frameId": row["frameId"],
                        "timestampSec": row["timestampSec"],
                        "filename": row["filename"],
                        "room": "unknown",
                        "uncertain": True,
                        "containsPerson": True,
                        "provider": "gemini_error_fallback",
                        "model": self.model,
                    })
        results.sort(key=lambda item: item["frameId"])
        return {
            "results": results,
            "apiCalls": api_calls,
            "errors": errors,
            "elapsedSec": round(time.perf_counter() - started, 3),
        }
