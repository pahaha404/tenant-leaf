import unittest
from uuid import UUID

from tenant_leaf_worker.contracts import parse_result


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


if __name__ == "__main__":
    unittest.main()
