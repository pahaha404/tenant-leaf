from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import tempfile
import time
from dataclasses import dataclass
from pathlib import Path
from urllib.parse import urlparse
from uuid import UUID, uuid4

from tenant_leaf_worker.runtime import validate_python_runtime

validate_python_runtime()

import psycopg
from dotenv import load_dotenv
from minio import Minio
from PIL import Image
from psycopg.conninfo import make_conninfo

from tenant_leaf_worker.contracts import DetectionResult, parse_result


MAX_JPEG_BYTES = 2_097_152


@dataclass(frozen=True)
class Settings:
    database_dsn: str
    minio_endpoint: str
    minio_access_key: str
    minio_secret_key: str
    minio_bucket: str
    ai_module_root: Path
    poll_seconds: float

    @classmethod
    def from_environment(cls) -> "Settings":
        repository_root = Path(__file__).resolve().parents[3]
        load_dotenv(repository_root / ".env")
        database_dsn = os.getenv("AI_WORKER_DATABASE_DSN") or make_conninfo(
            dbname=os.getenv("POSTGRES_DB", "tenant_leaf"),
            user=os.getenv("POSTGRES_USER", "tenant_leaf"),
            password=os.getenv("POSTGRES_PASSWORD", "tenant_leaf_local"),
            host=os.getenv("POSTGRES_HOST", "localhost"),
            port=os.getenv("POSTGRES_PORT", "5432"),
        )
        return cls(
            database_dsn=database_dsn,
            minio_endpoint=os.getenv("OBJECT_STORAGE_ENDPOINT", "http://localhost:9000"),
            minio_access_key=os.getenv("OBJECT_STORAGE_ACCESS_KEY", "tenant_leaf"),
            minio_secret_key=os.getenv("OBJECT_STORAGE_SECRET_KEY", "tenant_leaf_minio_local"),
            minio_bucket=os.getenv("OBJECT_STORAGE_BUCKET", "tenant-leaf-media"),
            ai_module_root=Path(
                os.getenv("AI_MODEL_MODULE_ROOT", str(repository_root / "ai" / "ai-video-defect")),
            ).resolve(),
            poll_seconds=float(os.getenv("AI_WORKER_POLL_SECONDS", "2")),
        )


@dataclass(frozen=True)
class ClaimedJob:
    job_id: UUID
    media_id: UUID
    owner_id: UUID
    inspection_id: UUID
    storage_key: str
    width: int
    height: int


class MediaAnalysisWorker:
    def __init__(self, settings: Settings):
        self.settings = settings
        required_model_files = (
            settings.ai_module_root / "training" / "process_images_two_stage.py",
            settings.ai_module_root / "models" / "active" / "two_stage_negative_rot4" / "binary" / "best.pt",
            settings.ai_module_root / "models" / "active" / "two_stage_negative_rot4" / "multiclass" / "best.pt",
        )
        if not all(path.is_file() for path in required_model_files):
            raise RuntimeError("AI model code or weights are not deployed")
        if settings.poll_seconds <= 0:
            raise ValueError("AI_WORKER_POLL_SECONDS must be greater than zero")
        parsed = urlparse(settings.minio_endpoint)
        if parsed.scheme not in {"http", "https"} or not parsed.netloc:
            raise ValueError("OBJECT_STORAGE_ENDPOINT must be an http(s) URL")
        self.storage = Minio(
            parsed.netloc,
            access_key=settings.minio_access_key,
            secret_key=settings.minio_secret_key,
            secure=parsed.scheme == "https",
        )

    def run_once(self) -> bool:
        job = self._claim()
        if job is None:
            return False
        try:
            with tempfile.TemporaryDirectory(prefix=f"tenant-leaf-{job.job_id}-") as temp:
                temp_root = Path(temp)
                input_path = temp_root / f"{job.media_id}.jpg"
                output_path = temp_root / "output"
                self.storage.fget_object(self.settings.minio_bucket, job.storage_key, str(input_path))
                self._validate_jpeg(input_path, job.width, job.height)
                result = self._run_model(job, input_path, output_path)
                model_version, detections = parse_result(result, job.media_id)
                crop_keys = self._upload_crops(job, detections)
                self._complete(job, model_version, detections, crop_keys)
        except Exception as error:  # worker boundary: record sanitized failure, then continue polling
            self._fail(job, type(error).__name__, str(error))
        return True

    def _claim(self) -> ClaimedJob | None:
        with psycopg.connect(self.settings.database_dsn) as connection:
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    WITH candidate AS (
                        SELECT j.id
                        FROM media_analysis_jobs j
                        JOIN media m ON m.id = j.media_id
                        WHERE j.status = 'QUEUED'
                          AND j.available_at <= NOW()
                          AND m.upload_status = 'UPLOADED'
                          AND m.deleted_at IS NULL
                        ORDER BY j.created_at
                        FOR UPDATE OF j SKIP LOCKED
                        LIMIT 1
                    )
                    UPDATE media_analysis_jobs j
                    SET status = 'ANALYZING',
                        attempt_count = attempt_count + 1,
                        started_at = NOW(),
                        updated_at = NOW(),
                        failure_code = NULL,
                        failure_message = NULL
                    FROM candidate
                    WHERE j.id = candidate.id
                    RETURNING j.id, j.media_id
                    """,
                )
                row = cursor.fetchone()
                if row is None:
                    return None
                job_id, media_id = row
                cursor.execute(
                    """
                    UPDATE media SET analysis_status = 'ANALYZING', updated_at = NOW()
                    WHERE id = %s
                    RETURNING owner_id, inspection_id, storage_key, width, height
                    """,
                    (media_id,),
                )
                media = cursor.fetchone()
                if media is None:
                    raise RuntimeError("Queued media no longer exists")
                owner_id, inspection_id, storage_key, width, height = media
                return ClaimedJob(job_id, media_id, owner_id, inspection_id, storage_key, width, height)

    def _validate_jpeg(self, path: Path, expected_width: int, expected_height: int) -> None:
        size = path.stat().st_size
        if size <= 0 or size > MAX_JPEG_BYTES:
            raise ValueError("Stored JPEG size is outside the allowed range")
        with Image.open(path) as image:
            image.verify()
        with Image.open(path) as image:
            if image.format != "JPEG":
                raise ValueError("Stored object is not a JPEG")
            if image.width != expected_width or image.height != expected_height:
                raise ValueError("Stored JPEG dimensions do not match media metadata")

    def _run_model(self, job: ClaimedJob, input_path: Path, output_path: Path) -> dict:
        command = [
            sys.executable,
            "-m",
            "training.process_images_two_stage",
            "--input",
            str(input_path),
            "--media-id",
            str(job.media_id),
            "--job-id",
            str(job.job_id),
            "--output",
            str(output_path),
        ]
        completed = subprocess.run(
            command,
            cwd=self.settings.ai_module_root,
            check=False,
            capture_output=True,
            text=True,
            timeout=600,
        )
        if completed.returncode != 0:
            raise RuntimeError("AI model process failed")
        result_path = output_path / "result.json"
        if not result_path.is_file():
            raise RuntimeError("AI model did not create result.json")
        return json.loads(result_path.read_text(encoding="utf-8"))

    def _upload_crops(self, job: ClaimedJob, detections: list[DetectionResult]) -> dict[int, str]:
        crop_keys: dict[int, str] = {}
        for index, detection in enumerate(detections, start=1):
            if detection.crop_path is None:
                continue
            crop_path = Path(detection.crop_path)
            if not crop_path.is_file():
                raise ValueError("AI crop file does not exist")
            key = f"derived/{job.owner_id}/{job.inspection_id}/{job.media_id}/{index:03d}.jpg"
            self.storage.fput_object(
                self.settings.minio_bucket,
                key,
                str(crop_path),
                content_type="image/jpeg",
            )
            crop_keys[index] = key
        return crop_keys

    def _complete(
        self,
        job: ClaimedJob,
        model_version: str,
        detections: list[DetectionResult],
        crop_keys: dict[int, str],
    ) -> None:
        with psycopg.connect(self.settings.database_dsn) as connection:
            with connection.cursor() as cursor:
                cursor.execute("DELETE FROM media_analysis_detections WHERE job_id = %s", (job.job_id,))
                for index, detection in enumerate(detections, start=1):
                    cursor.execute(
                        """
                        INSERT INTO media_analysis_detections (
                            id, job_id, media_id, class_id, label, confidence,
                            box_left, box_top, box_right, box_bottom, model_version,
                            crop_storage_key, crop_width, crop_height, created_at
                        ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW())
                        """,
                        (
                            uuid4(), job.job_id, job.media_id, detection.class_id, detection.label,
                            detection.confidence, detection.left, detection.top, detection.right,
                            detection.bottom, model_version, crop_keys.get(index),
                            detection.crop_width, detection.crop_height,
                        ),
                    )
                cursor.execute(
                    """
                    UPDATE media_analysis_jobs
                    SET status = 'COMPLETED', completed_at = NOW(), model_version = %s, updated_at = NOW()
                    WHERE id = %s AND status = 'ANALYZING'
                    """,
                    (model_version, job.job_id),
                )
                cursor.execute(
                    "UPDATE media SET analysis_status = 'COMPLETED', updated_at = NOW() WHERE id = %s",
                    (job.media_id,),
                )
                self._refresh_inspection(cursor, job.inspection_id)

    def _fail(self, job: ClaimedJob, code: str, message: str) -> None:
        safe_code = code[:64]
        safe_message = message.replace("\n", " ")[:500]
        with psycopg.connect(self.settings.database_dsn) as connection:
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    UPDATE media_analysis_jobs
                    SET status = 'FAILED', completed_at = NOW(), failure_code = %s,
                        failure_message = %s, updated_at = NOW()
                    WHERE id = %s
                    """,
                    (safe_code, safe_message, job.job_id),
                )
                cursor.execute(
                    "UPDATE media SET analysis_status = 'FAILED', updated_at = NOW() WHERE id = %s",
                    (job.media_id,),
                )
                self._refresh_inspection(cursor, job.inspection_id)

    @staticmethod
    def _refresh_inspection(cursor, inspection_id: UUID) -> None:
        cursor.execute(
            """
            UPDATE inspections i
            SET analysis_status = CASE
                WHEN EXISTS (
                    SELECT 1 FROM media m WHERE m.inspection_id = i.id AND m.deleted_at IS NULL
                      AND m.upload_status <> 'UPLOADED'
                ) THEN 'UPLOADING'
                WHEN EXISTS (
                    SELECT 1 FROM media m WHERE m.inspection_id = i.id AND m.deleted_at IS NULL
                      AND m.analysis_status = 'ANALYZING'
                ) THEN 'ANALYZING'
                WHEN NOT EXISTS (
                    SELECT 1 FROM media m WHERE m.inspection_id = i.id AND m.deleted_at IS NULL
                      AND m.analysis_status <> 'COMPLETED'
                ) THEN 'COMPLETED'
                WHEN EXISTS (
                    SELECT 1 FROM media m WHERE m.inspection_id = i.id AND m.deleted_at IS NULL
                      AND m.analysis_status = 'COMPLETED'
                ) AND EXISTS (
                    SELECT 1 FROM media m WHERE m.inspection_id = i.id AND m.deleted_at IS NULL
                      AND m.analysis_status = 'FAILED'
                ) THEN 'PARTIAL_COMPLETED'
                WHEN NOT EXISTS (
                    SELECT 1 FROM media m WHERE m.inspection_id = i.id AND m.deleted_at IS NULL
                      AND m.analysis_status <> 'FAILED'
                ) THEN 'FAILED'
                ELSE 'QUEUED'
            END
            WHERE i.id = %s
            """,
            (inspection_id,),
        )


def main() -> None:
    parser = argparse.ArgumentParser(description="Process queued Tenant Leaf JPEG analysis jobs")
    parser.add_argument("--once", action="store_true", help="Process at most one job and exit")
    args = parser.parse_args()
    worker = MediaAnalysisWorker(Settings.from_environment())
    if args.once:
        worker.run_once()
        return
    while True:
        if not worker.run_once():
            time.sleep(worker.settings.poll_seconds)


if __name__ == "__main__":
    main()
