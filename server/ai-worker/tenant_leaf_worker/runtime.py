from __future__ import annotations

import sys
from collections.abc import Sequence


def validate_python_runtime(version: Sequence[int] | None = None) -> None:
    """Reject Python releases unsupported by the pinned AI runtime."""
    current = tuple(version or sys.version_info[:3])
    major_minor = current[:2]
    if major_minor < (3, 11) or major_minor > (3, 14):
        raise RuntimeError("AI Worker requires CPython 3.11 through 3.14")
    if current[:3] == (3, 14, 1):
        raise RuntimeError(
            "Python 3.14.1 is not supported by torchvision; use Python 3.14.0 or 3.14.2+",
        )
