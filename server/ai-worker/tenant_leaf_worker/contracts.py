from __future__ import annotations

from dataclasses import dataclass
from typing import Any
from uuid import UUID


LABELS_BY_CLASS_ID = {
    0: "crack",
    1: "mold",
    2: "peeling",
    3: "water_damage",
    4: "tile_damage",
    5: "hole",
    6: "tile_crack",
    7: "paint_drips",
    8: "pin_hole",
    9: "surface_defect",
    10: "stain",
    11: "trowel_mark",
    12: "other",
}


@dataclass(frozen=True)
class DetectionResult:
    class_id: int
    label: str
    confidence: float
    left: float
    top: float
    right: float
    bottom: float
    crop_path: str | None
    crop_width: int | None
    crop_height: int | None


ROOM_TO_ZONE = {
    "bathroom": "BATHROOM",
    "kitchen": "KITCHEN",
    "living_room": "LIVING_ROOM",
    "unknown": "UNKNOWN",
}


@dataclass(frozen=True)
class BatchImageResult:
    media_id: UUID
    zone: str
    zone_uncertain: bool
    zone_model_version: str
    contains_person: bool
    detections: tuple[DetectionResult, ...]


def parse_result(payload: dict[str, Any], expected_media_id: UUID) -> tuple[str, list[DetectionResult]]:
    if payload.get("status") != "completed":
        raise ValueError("AI result status must be completed")
    if payload.get("mediaId") != str(expected_media_id):
        raise ValueError("AI result mediaId does not match the queued media")

    model_version = payload.get("modelVersion")
    if not isinstance(model_version, str) or not model_version.strip():
        raise ValueError("AI result modelVersion is required")

    images = payload.get("images")
    if not isinstance(images, list) or len(images) != 1:
        raise ValueError("A media analysis job must return exactly one image result")
    image = images[0].get("image") if isinstance(images[0], dict) else None
    if not isinstance(image, dict):
        raise ValueError("AI result image metadata is required")
    width, height = image.get("width"), image.get("height")
    if not isinstance(width, int) or not isinstance(height, int) or width <= 0 or height <= 0:
        raise ValueError("AI result image dimensions are invalid")

    crop_by_box = _crop_metadata(payload.get("observations"))
    raw_detections = images[0].get("detections")
    if not isinstance(raw_detections, list):
        raise ValueError("AI result detections must be an array")

    detections = [
        _parse_detection(item, width, height, crop_by_box)
        for item in raw_detections
    ]
    return model_version.strip(), detections


def parse_batch_result(
    payload: dict[str, Any],
    expected_media_ids: set[UUID],
) -> tuple[str, list[BatchImageResult]]:
    if payload.get("status") != "completed":
        raise ValueError("AI batch result status must be completed")

    model_version = payload.get("modelVersion")
    if not isinstance(model_version, str) or not model_version.strip():
        raise ValueError("AI batch result modelVersion is required")

    room_classification = payload.get("roomClassification")
    default_zone_model = room_classification.get("model") if isinstance(room_classification, dict) else None
    images = payload.get("images")
    if not isinstance(images, list):
        raise ValueError("AI batch result images must be an array")

    parsed: list[BatchImageResult] = []
    seen: set[UUID] = set()
    for item in images:
        if not isinstance(item, dict):
            raise ValueError("Each AI batch image result must be an object")
        try:
            media_id = UUID(str(item.get("imageId")))
        except (TypeError, ValueError) as error:
            raise ValueError("AI batch imageId must be a media UUID") from error
        if media_id not in expected_media_ids:
            raise ValueError("AI batch result contains an unexpected mediaId")
        if media_id in seen:
            raise ValueError("AI batch result contains a duplicate mediaId")
        seen.add(media_id)

        image = item.get("image")
        if not isinstance(image, dict):
            raise ValueError("AI batch image metadata is required")
        width, height = image.get("width"), image.get("height")
        if not isinstance(width, int) or not isinstance(height, int) or width <= 0 or height <= 0:
            raise ValueError("AI batch image dimensions are invalid")

        room = item.get("room")
        if not isinstance(room, dict):
            raise ValueError("AI batch room metadata is required")
        stable_room = room.get("stable")
        if stable_room not in ROOM_TO_ZONE:
            raise ValueError("AI batch stable room is unsupported")
        uncertain = room.get("uncertain")
        if not isinstance(uncertain, bool):
            raise ValueError("AI batch room uncertain flag is required")
        contains_person = room.get("containsPerson", True)
        if not isinstance(contains_person, bool):
            raise ValueError("AI batch room containsPerson flag must be boolean")
        zone_model = room.get("model") or default_zone_model
        if not isinstance(zone_model, str) or not zone_model.strip():
            raise ValueError("AI batch room model is required")

        raw_detections = item.get("detections")
        if not isinstance(raw_detections, list):
            raise ValueError("AI batch detections must be an array")
        detections = tuple(
            _parse_detection(detection, width, height, {})
            for detection in raw_detections
        )
        parsed.append(BatchImageResult(
            media_id=media_id,
            zone=ROOM_TO_ZONE[stable_room],
            zone_uncertain=uncertain or stable_room == "unknown",
            zone_model_version=zone_model.strip(),
            contains_person=contains_person,
            detections=detections,
        ))

    if seen != expected_media_ids:
        raise ValueError("AI batch result does not contain every claimed mediaId")
    return model_version.strip(), parsed


def _parse_detection(
    item: Any,
    image_width: int,
    image_height: int,
    crop_by_box: dict[tuple[float, float, float, float], tuple[str, int, int]],
) -> DetectionResult:
    if not isinstance(item, dict):
        raise ValueError("Each detection must be an object")

    class_id = item.get("classId")
    label = item.get("label")
    if not isinstance(class_id, int) or not 0 <= class_id <= 12:
        raise ValueError("Detection classId must be between 0 and 12")
    if not isinstance(label, str) or not label:
        raise ValueError("Detection label is required")
    if LABELS_BY_CLASS_ID[class_id] != label:
        raise ValueError("Detection label does not match classId")

    confidence = item.get("confidence")
    if not isinstance(confidence, (int, float)) or not 0 <= float(confidence) <= 1:
        raise ValueError("Detection confidence must be between 0 and 1")

    box = item.get("box")
    if not isinstance(box, dict):
        raise ValueError("Detection box is required")
    coordinates = tuple(float(box[name]) for name in ("left", "top", "right", "bottom"))
    left, top, right, bottom = coordinates
    if left < 0 or top < 0 or right <= left or bottom <= top:
        raise ValueError("Detection box coordinates are invalid")
    if right > image_width or bottom > image_height:
        raise ValueError("Detection box exceeds the source image")

    crop = crop_by_box.get(coordinates)
    return DetectionResult(
        class_id=class_id,
        label=label,
        confidence=float(confidence),
        left=left,
        top=top,
        right=right,
        bottom=bottom,
        crop_path=crop[0] if crop else None,
        crop_width=crop[1] if crop else None,
        crop_height=crop[2] if crop else None,
    )


def _crop_metadata(raw_observations: Any) -> dict[tuple[float, float, float, float], tuple[str, int, int]]:
    if not isinstance(raw_observations, list):
        return {}
    result: dict[tuple[float, float, float, float], tuple[str, int, int]] = {}
    for item in raw_observations:
        if not isinstance(item, dict) or not item.get("cropPath"):
            continue
        try:
            box = item["box"]
            key = tuple(float(box[name]) for name in ("left", "top", "right", "bottom"))
            crop_image = item["cropImage"]
            crop_width, crop_height = int(crop_image["width"]), int(crop_image["height"])
            if crop_width > 0 and crop_height > 0:
                result[key] = (str(item["cropPath"]), crop_width, crop_height)
        except (KeyError, TypeError, ValueError):
            continue
    return result
