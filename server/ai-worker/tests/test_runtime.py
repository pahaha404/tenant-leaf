import unittest

from tenant_leaf_worker.runtime import validate_python_runtime


class PythonRuntimeTests(unittest.TestCase):
    def test_python_314_is_supported(self):
        validate_python_runtime((3, 14, 0))
        validate_python_runtime((3, 14, 2))

    def test_python_3141_is_rejected_for_torchvision_compatibility(self):
        with self.assertRaisesRegex(RuntimeError, "3.14.1"):
            validate_python_runtime((3, 14, 1))

    def test_future_unverified_python_is_rejected(self):
        with self.assertRaisesRegex(RuntimeError, "3.11 through 3.14"):
            validate_python_runtime((3, 15, 0))


if __name__ == "__main__":
    unittest.main()
