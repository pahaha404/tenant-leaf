import unittest
from uuid import UUID

from tenant_leaf_worker.contracts import parse_batch_result, parse_result


MEDIA_ID = UUID("4ae76a4f-4dd9-4380-949a-8d80b47dfaa2")


class ResultContractTests(unittest.TestCase):
    def test_empty_detection_result_is_successful(self):
        model_version, detections = parse_result(
            {
                "status": "completed",
                "mediaId": str(MEDIA_ID),
                "modelVersion": "two_stage_negative_rot4",
                "images": [{"image": {"width": 640, "height": 480}, "detections": []}],
                "observations": [],
            },
            MEDIA_ID,
        )
        self.assertEqual("two_stage_negative_rot4", model_version)
        self.assertEqual([], detections)

    def test_other_candidate_uses_class_id_12(self):
        _, detections = parse_result(
            {
                "status": "completed",
                "mediaId": str(MEDIA_ID),
                "modelVersion": "two_stage_negative_rot4",
                "images": [{
                    "image": {"width": 640, "height": 480},
                    "detections": [{
                        "classId": 12,
                        "label": "other",
                        "confidence": 0.78,
                        "box": {"left": 10, "top": 20, "right": 100, "bottom": 120},
                    }],
                }],
                "observations": [],
            },
            MEDIA_ID,
        )
        self.assertEqual(12, detections[0].class_id)
        self.assertEqual("other", detections[0].label)

    def test_box_outside_image_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "exceeds"):
            parse_result(
                {
                    "status": "completed",
                    "mediaId": str(MEDIA_ID),
                    "modelVersion": "v1",
                    "images": [{
                        "image": {"width": 640, "height": 480},
                        "detections": [{
                            "classId": 1,
                            "label": "mold",
                            "confidence": 0.7,
                            "box": {"left": 10, "top": 20, "right": 700, "bottom": 120},
                        }],
                    }],
                },
                MEDIA_ID,
            )

    def test_label_must_match_class_id(self):
        with self.assertRaisesRegex(ValueError, "does not match"):
            parse_result(
                {
                    "status": "completed",
                    "mediaId": str(MEDIA_ID),
                    "modelVersion": "v1",
                    "images": [{
                        "image": {"width": 640, "height": 480},
                        "detections": [{
                            "classId": 1,
                            "label": "crack",
                            "confidence": 0.7,
                            "box": {"left": 10, "top": 20, "right": 100, "bottom": 120},
                        }],
                    }],
                },
                MEDIA_ID,
            )

    def test_batch_result_maps_rooms_and_keeps_verified_detections(self):
        second_media_id = UUID("aa8a41ca-17c5-4bf0-83f2-3289a44bd664")
        third_media_id = UUID("93afad48-a5ea-4143-92e3-e210e79ed010")
        fourth_media_id = UUID("b37bc38c-091f-4302-831a-d7c1f68e60fc")
        model_version, images = parse_batch_result(
            {
                "status": "completed",
                "modelVersion": "two_stage_negative_rot4",
                "roomClassification": {"model": "gemini-3.5-flash-lite"},
                "images": [
                    {
                        "imageId": str(MEDIA_ID),
                        "image": {"width": 640, "height": 480},
                        "room": {
                            "stable": "bathroom",
                            "uncertain": False,
                            "model": "gemini-3.5-flash-lite",
                        },
                        "detections": [{
                            "classId": 1,
                            "label": "mold",
                            "confidence": 0.76,
                            "box": {"left": 10, "top": 20, "right": 100, "bottom": 120},
                        }],
                    },
                    {
                        "imageId": str(second_media_id),
                        "image": {"width": 1280, "height": 720},
                        "room": {
                            "stable": "unknown",
                            "uncertain": False,
                            "model": "gemini-3.5-flash-lite",
                        },
                        "detections": [],
                    },
                    {
                        "imageId": str(third_media_id),
                        "image": {"width": 1280, "height": 720},
                        "room": {
                            "stable": "kitchen",
                            "uncertain": False,
                            "model": "gemini-3.5-flash-lite",
                        },
                        "detections": [],
                    },
                    {
                        "imageId": str(fourth_media_id),
                        "image": {"width": 1280, "height": 720},
                        "room": {
                            "stable": "living_room",
                            "uncertain": False,
                            "model": "gemini-3.5-flash-lite",
                        },
                        "detections": [],
                    },
                ],
            },
            {MEDIA_ID, second_media_id, third_media_id, fourth_media_id},
        )

        self.assertEqual("two_stage_negative_rot4", model_version)
        self.assertEqual("BATHROOM", images[0].zone)
        self.assertFalse(images[0].zone_uncertain)
        self.assertEqual("mold", images[0].detections[0].label)
        self.assertEqual("UNKNOWN", images[1].zone)
        self.assertTrue(images[1].zone_uncertain)
        self.assertEqual("KITCHEN", images[2].zone)
        self.assertEqual("LIVING_ROOM", images[3].zone)

    def test_batch_result_requires_every_claimed_media(self):
        with self.assertRaisesRegex(ValueError, "every claimed"):
            parse_batch_result(
                {
                    "status": "completed",
                    "modelVersion": "v1",
                    "roomClassification": {"model": "gemini-3.5-flash-lite"},
                    "images": [],
                },
                {MEDIA_ID},
            )


if __name__ == "__main__":
    unittest.main()
