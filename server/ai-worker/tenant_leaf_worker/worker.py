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

from tenant_leaf_worker.contracts import BatchImageResult, DetectionResult, parse_batch_result


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
    model_timeout_seconds: int
    gemini_model: str
    gemini_defect_model: str
    gemini_reject_confidence: float

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
            model_timeout_seconds=int(os.getenv("AI_WORKER_MODEL_TIMEOUT_SECONDS", "1800")),
            gemini_model=os.getenv("AI_WORKER_GEMINI_MODEL", "gemini-3.5-flash-lite"),
            gemini_defect_model=os.getenv(
                "AI_WORKER_GEMINI_DEFECT_MODEL", "gemini-3.5-flash-lite"
            ),
            gemini_reject_confidence=float(
                os.getenv("AI_WORKER_GEMINI_REJECT_CONFIDENCE", "0.90")
            ),
        )


@dataclass(frozen=True)
class ClaimedMedia:
    job_id: UUID
    media_id: UUID
    owner_id: UUID
    inspection_id: UUID
    storage_key: str
    width: int
    height: int
    source_video_offset_ms: int


@dataclass(frozen=True)
class ClaimedBatch:
    inspection_id: UUID
    owner_id: UUID
    media: tuple[ClaimedMedia, ...]


class MediaAnalysisWorker:
    def __init__(self, settings: Settings):
        self.settings = settings
        required_model_files = (
            settings.ai_module_root / "training" / "process_image_batch_room_defect.py",
            settings.ai_module_root / "training" / "gemini_room_classifier.py",
            settings.ai_module_root / "training" / "gemini_defect_verifier.py",
            settings.ai_module_root / "models" / "active" / "two_stage_negative_rot4" / "binary" / "best.pt",
            settings.ai_module_root / "models" / "active" / "two_stage_negative_rot4" / "multiclass" / "best.pt",
        )
        if not all(path.is_file() for path in required_model_files):
            raise RuntimeError("AI batch model code or weights are not deployed")
        if not os.getenv("GEMINI_API_KEY"):
            raise RuntimeError("GEMINI_API_KEY is required for room and defect analysis")
        if settings.poll_seconds <= 0:
            raise ValueError("AI_WORKER_POLL_SECONDS must be greater than zero")
        if settings.model_timeout_seconds <= 0:
            raise ValueError("AI_WORKER_MODEL_TIMEOUT_SECONDS must be greater than zero")
        if not 0 <= settings.gemini_reject_confidence <= 1:
            raise ValueError("AI_WORKER_GEMINI_REJECT_CONFIDENCE must be between zero and one")
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
        batch = self._claim_batch()
        if batch is None:
            return False
        print(
            f"AI batch claimed inspection={batch.inspection_id} "
            f"media_count={len(batch.media)}",
            flush=True,
        )
        try:
            with tempfile.TemporaryDirectory(prefix=f"tenant-leaf-{batch.inspection_id}-") as temp:
                temp_root = Path(temp)
                input_path = temp_root / "input"
                output_path = temp_root / "output"
                manifest_path = temp_root / "manifest.json"
                input_path.mkdir()
                self._download_batch(batch, input_path, manifest_path)
                result = self._run_model(batch, input_path, manifest_path, output_path)
                model_version, images = parse_batch_result(
                    result,
                    {item.media_id for item in batch.media},
                )
                self._complete_batch(batch, model_version, images)
                print(
                    f"AI batch completed inspection={batch.inspection_id} "
                    f"media_count={len(batch.media)} model_version={model_version}",
                    flush=True,
                )
        except Exception as error:  # worker boundary: record sanitized failure, then continue polling
            error_code = type(error).__name__
            error_message = self._sanitize_error(str(error))
            print(
                f"AI batch failed inspection={batch.inspection_id} "
                f"media_count={len(batch.media)} code={error_code} message={error_message}",
                file=sys.stderr,
                flush=True,
            )
            self._fail_batch(batch, error_code, error_message)
        return True

    def _claim_batch(self) -> ClaimedBatch | None:
        with psycopg.connect(self.settings.database_dsn) as connection:
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    SELECT i.id
                    FROM inspections i
                    WHERE i.media_finalized_at IS NOT NULL
                      AND EXISTS (
                          SELECT 1
                          FROM media_analysis_jobs j
                          JOIN media m ON m.id = j.media_id
                          WHERE m.inspection_id = i.id
                            AND j.status = 'QUEUED'
                            AND j.available_at <= NOW()
                            AND m.upload_status = 'UPLOADED'
                            AND m.deleted_at IS NULL
                      )
                    ORDER BY (
                        SELECT MIN(j.created_at)
                        FROM media_analysis_jobs j
                        JOIN media m ON m.id = j.media_id
                        WHERE m.inspection_id = i.id
                          AND j.status = 'QUEUED'
                    )
                    FOR UPDATE OF i SKIP LOCKED
                    LIMIT 1
                    """,
                )
                inspection_row = cursor.fetchone()
                if inspection_row is None:
                    return None
                inspection_id = inspection_row[0]

                cursor.execute(
                    """
                    UPDATE media_analysis_jobs j
                    SET status = 'ANALYZING',
                        attempt_count = attempt_count + 1,
                        started_at = NOW(),
                        completed_at = NULL,
                        updated_at = NOW(),
                        failure_code = NULL,
                        failure_message = NULL,
                        model_version = NULL
                    FROM media m
                    WHERE j.media_id = m.id
                      AND m.inspection_id = %s
                      AND j.status = 'QUEUED'
                      AND j.available_at <= NOW()
                      AND m.upload_status = 'UPLOADED'
                      AND m.deleted_at IS NULL
                    RETURNING j.id, j.media_id, m.owner_id, m.inspection_id,
                              m.storage_key, m.width, m.height, m.source_video_offset_ms
                    """,
                    (inspection_id,),
                )
                rows = cursor.fetchall()
                if not rows:
                    return None
                claimed = tuple(sorted(
                    (ClaimedMedia(*row) for row in rows),
                    key=lambda item: (item.source_video_offset_ms, str(item.media_id)),
                ))
                owner_ids = {item.owner_id for item in claimed}
                if len(owner_ids) != 1:
                    raise RuntimeError("An inspection batch contains multiple owners")
                for item in claimed:
                    cursor.execute(
                        "UPDATE media SET analysis_status = 'ANALYZING', updated_at = NOW() WHERE id = %s",
                        (item.media_id,),
                    )
                self._refresh_inspection(cursor, inspection_id)
                return ClaimedBatch(inspection_id, claimed[0].owner_id, claimed)

    def _download_batch(
        self,
        batch: ClaimedBatch,
        input_path: Path,
        manifest_path: Path,
    ) -> None:
        manifest_images = []
        for sequence_index, item in enumerate(batch.media):
            filename = f"{sequence_index + 1:06d}.jpg"
            local_path = input_path / filename
            self.storage.fget_object(self.settings.minio_bucket, item.storage_key, str(local_path))
            self._validate_jpeg(local_path, item.width, item.height)
            manifest_images.append({
                "filename": filename,
                "imageId": str(item.media_id),
                "sequenceIndex": sequence_index,
                "sourceVideoOffsetMs": item.source_video_offset_ms,
                "timestampSec": item.source_video_offset_ms / 1000.0,
            })
        manifest_path.write_text(
            json.dumps({"inspectionId": str(batch.inspection_id), "images": manifest_images}, indent=2),
            encoding="utf-8",
        )

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

    def _run_model(
        self,
        batch: ClaimedBatch,
        input_path: Path,
        manifest_path: Path,
        output_path: Path,
    ) -> dict:
        command = [
            sys.executable,
            "-m",
            "training.process_image_batch_room_defect",
            "--input",
            str(input_path),
            "--manifest",
            str(manifest_path),
            "--job-id",
            str(batch.inspection_id),
            "--output",
            str(output_path),
            "--room-provider",
            "gemini",
            "--defect-verifier",
            "gemini",
            "--gemini-model",
            self.settings.gemini_model,
            "--gemini-defect-model",
            self.settings.gemini_defect_model,
            "--gemini-reject-confidence",
            str(self.settings.gemini_reject_confidence),
        ]
        completed = subprocess.run(
            command,
            cwd=self.settings.ai_module_root,
            check=False,
            capture_output=True,
            text=True,
            timeout=self.settings.model_timeout_seconds,
        )
        if completed.returncode != 0:
            result_path = output_path / "result.json"
            if result_path.is_file():
                try:
                    failed_result = json.loads(result_path.read_text(encoding="utf-8"))
                    failure = failed_result.get("error") or {}
                    failure_type = failure.get("type") or "UnknownError"
                    failure_message = failure.get("message") or "No failure message"
                    raise RuntimeError(
                        f"AI batch model process failed (exit={completed.returncode}): "
                        f"{failure_type}: {failure_message}"
                    )
                except json.JSONDecodeError:
                    pass
            diagnostic = completed.stdout.strip() or completed.stderr.strip()
            if diagnostic:
                diagnostic = " ".join(diagnostic.splitlines()[-20:])
                raise RuntimeError(
                    f"AI batch model process failed (exit={completed.returncode}): {diagnostic}"
                )
            raise RuntimeError(
                f"AI batch model process failed (exit={completed.returncode}, no diagnostic output)"
            )
        result_path = output_path / "result.json"
        if not result_path.is_file():
            raise RuntimeError("AI batch model did not create result.json")
        return json.loads(result_path.read_text(encoding="utf-8"))

    def _complete_batch(
        self,
        batch: ClaimedBatch,
        model_version: str,
        results: list[BatchImageResult],
    ) -> None:
        jobs_by_media = {item.media_id: item for item in batch.media}
        with psycopg.connect(self.settings.database_dsn) as connection:
            with connection.cursor() as cursor:
                for result in results:
                    claimed = jobs_by_media[result.media_id]
                    cursor.execute(
                        "DELETE FROM media_analysis_detections WHERE job_id = %s",
                        (claimed.job_id,),
                    )
                    self._insert_detections(cursor, claimed, model_version, result.detections)
                    cursor.execute(
                        """
                        UPDATE media_analysis_jobs
                        SET status = 'COMPLETED', completed_at = NOW(), model_version = %s,
                            failure_code = NULL, failure_message = NULL, updated_at = NOW()
                        WHERE id = %s AND status = 'ANALYZING'
                        """,
                        (model_version, claimed.job_id),
                    )
                    cursor.execute(
                        """
                        UPDATE media
                        SET analysis_status = 'COMPLETED', ai_zone = %s,
                            zone_uncertain = %s, zone_model_version = %s,
                            zone_confidence = NULL, contains_person = %s, updated_at = NOW()
                        WHERE id = %s
                        """,
                        (
                            result.zone,
                            result.zone_uncertain,
                            result.zone_model_version,
                            result.contains_person,
                            result.media_id,
                        ),
                    )
                self._refresh_inspection(cursor, batch.inspection_id)

    @staticmethod
    def _insert_detections(
        cursor,
        claimed: ClaimedMedia,
        model_version: str,
        detections: tuple[DetectionResult, ...],
    ) -> None:
        for detection in detections:
            cursor.execute(
                """
                INSERT INTO media_analysis_detections (
                    id, job_id, media_id, class_id, label, confidence,
                    box_left, box_top, box_right, box_bottom, model_version,
                    crop_storage_key, crop_width, crop_height, created_at
                ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NULL, NULL, NULL, NOW())
                """,
                (
                    uuid4(), claimed.job_id, claimed.media_id, detection.class_id,
                    detection.label, detection.confidence, detection.left, detection.top,
                    detection.right, detection.bottom, model_version,
                ),
            )

    def _fail_batch(self, batch: ClaimedBatch, code: str, message: str) -> None:
        safe_code = code[:64]
        safe_message = self._sanitize_error(message)[:500]
        with psycopg.connect(self.settings.database_dsn) as connection:
            with connection.cursor() as cursor:
                for item in batch.media:
                    cursor.execute(
                        """
                        UPDATE media_analysis_jobs
                        SET status = 'FAILED', completed_at = NOW(), failure_code = %s,
                            failure_message = %s, updated_at = NOW()
                        WHERE id = %s AND status = 'ANALYZING'
                        """,
                        (safe_code, safe_message, item.job_id),
                    )
                    cursor.execute(
                        "UPDATE media SET analysis_status = 'FAILED', updated_at = NOW() WHERE id = %s",
                        (item.media_id,),
                    )
                self._refresh_inspection(cursor, batch.inspection_id)

    @staticmethod
    def _sanitize_error(message: str) -> str:
        sanitized = " ".join(message.splitlines())
        api_key = os.getenv("GEMINI_API_KEY")
        if api_key:
            sanitized = sanitized.replace(api_key, "[REDACTED]")
        return sanitized

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
                WHEN EXISTS (
                    SELECT 1 FROM media m WHERE m.inspection_id = i.id AND m.deleted_at IS NULL
                      AND m.analysis_status IN ('NOT_REQUESTED', 'QUEUED')
                ) THEN 'QUEUED'
                WHEN i.media_finalized_at IS NULL THEN 'UPLOADING'
                WHEN NOT EXISTS (
                    SELECT 1 FROM media m WHERE m.inspection_id = i.id AND m.deleted_at IS NULL
                ) THEN 'FAILED'
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
    parser = argparse.ArgumentParser(description="Process queued Tenant Leaf inspection batches")
    parser.add_argument("--once", action="store_true", help="Process at most one inspection batch and exit")
    args = parser.parse_args()
    worker = MediaAnalysisWorker(Settings.from_environment())
    print(
        f"Tenant Leaf AI Worker started poll_seconds={worker.settings.poll_seconds} "
        f"room_model={worker.settings.gemini_model} "
        f"defect_model={worker.settings.gemini_defect_model}",
        flush=True,
    )
    if args.once:
        if not worker.run_once():
            print("No eligible QUEUED inspection batch found", flush=True)
        return
    while True:
        if not worker.run_once():
            time.sleep(worker.settings.poll_seconds)


if __name__ == "__main__":
    main()
