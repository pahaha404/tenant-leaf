CREATE TABLE media_analysis_jobs (
    id UUID PRIMARY KEY,
    media_id UUID NOT NULL UNIQUE REFERENCES media(id) ON DELETE RESTRICT,
    status VARCHAR(24) NOT NULL CHECK (status IN ('QUEUED', 'ANALYZING', 'COMPLETED', 'FAILED')),
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    available_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    failure_code VARCHAR(64),
    failure_message VARCHAR(500),
    model_version VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_media_analysis_job_result CHECK (
        (status = 'QUEUED' AND started_at IS NULL AND completed_at IS NULL)
        OR
        (status = 'ANALYZING' AND started_at IS NOT NULL AND completed_at IS NULL)
        OR
        (status = 'COMPLETED' AND completed_at IS NOT NULL AND model_version IS NOT NULL)
        OR
        (status = 'FAILED' AND completed_at IS NOT NULL AND failure_code IS NOT NULL)
    )
);

CREATE INDEX idx_media_analysis_jobs_claim
    ON media_analysis_jobs (status, available_at, created_at);

CREATE TABLE media_analysis_detections (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES media_analysis_jobs(id) ON DELETE CASCADE,
    media_id UUID NOT NULL REFERENCES media(id) ON DELETE RESTRICT,
    class_id INTEGER NOT NULL CHECK (class_id BETWEEN 0 AND 12),
    label VARCHAR(64) NOT NULL,
    confidence DOUBLE PRECISION NOT NULL CHECK (confidence BETWEEN 0 AND 1),
    box_left DOUBLE PRECISION NOT NULL,
    box_top DOUBLE PRECISION NOT NULL,
    box_right DOUBLE PRECISION NOT NULL,
    box_bottom DOUBLE PRECISION NOT NULL,
    model_version VARCHAR(128) NOT NULL,
    crop_storage_key VARCHAR(512),
    crop_width INTEGER,
    crop_height INTEGER,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_media_detection_box CHECK (
        box_left >= 0 AND box_top >= 0 AND
        box_right > box_left AND box_bottom > box_top
    ),
    CONSTRAINT ck_media_detection_crop CHECK (
        (crop_storage_key IS NULL AND crop_width IS NULL AND crop_height IS NULL)
        OR
        (crop_storage_key IS NOT NULL AND crop_width IS NOT NULL AND crop_height IS NOT NULL
            AND crop_width > 0 AND crop_height > 0)
    )
);

CREATE INDEX idx_media_analysis_detections_media
    ON media_analysis_detections (media_id, confidence DESC);
