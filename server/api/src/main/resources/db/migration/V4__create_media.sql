CREATE TABLE media (
    id UUID PRIMARY KEY,
    inspection_id UUID NOT NULL REFERENCES inspections(id) ON DELETE RESTRICT,
    owner_id UUID NOT NULL,
    client_media_id UUID NOT NULL,
    zone VARCHAR(32) NOT NULL,
    media_type VARCHAR(16) NOT NULL CHECK (media_type = 'PHOTO'),
    content_type VARCHAR(32) NOT NULL CHECK (content_type = 'image/jpeg'),
    declared_file_size BIGINT NOT NULL CHECK (declared_file_size BETWEEN 1 AND 1048576),
    actual_file_size BIGINT,
    width INTEGER NOT NULL CHECK (width > 0),
    height INTEGER NOT NULL CHECK (height > 0),
    source_video_id UUID NOT NULL,
    source_video_offset_ms BIGINT NOT NULL CHECK (source_video_offset_ms >= 0),
    frame_origin VARCHAR(48) NOT NULL CHECK (frame_origin = 'POST_RECORDING_EXTRACTION'),
    capture_source VARCHAR(32) NOT NULL,
    captured_at TIMESTAMPTZ NOT NULL,
    storage_key VARCHAR(512) NOT NULL UNIQUE,
    upload_status VARCHAR(24) NOT NULL,
    analysis_status VARCHAR(24) NOT NULL,
    upload_attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (upload_attempt_count >= 0),
    uploaded_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_media_inspection_client UNIQUE (inspection_id, client_media_id)
);

CREATE INDEX idx_media_inspection_created ON media (inspection_id, created_at DESC);
CREATE INDEX idx_media_owner ON media (owner_id);

CREATE TABLE api_idempotency_records (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    operation VARCHAR(64) NOT NULL,
    resource_path VARCHAR(512) NOT NULL,
    idempotency_key UUID NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_api_idempotency UNIQUE (owner_id, operation, resource_path, idempotency_key)
);
