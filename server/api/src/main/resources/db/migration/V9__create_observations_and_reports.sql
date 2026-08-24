CREATE TABLE observation_thresholds (
    model_version VARCHAR(128) NOT NULL,
    class_id INTEGER NOT NULL CHECK (class_id BETWEEN 0 AND 12),
    minimum_confidence DOUBLE PRECISION NOT NULL CHECK (minimum_confidence BETWEEN 0 AND 1),
    configured_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (model_version, class_id)
);

-- MVP 데모용 잠정 표시 기준이다. 실제 현장 표본 검증 후 모델 버전별로 교체한다.
INSERT INTO observation_thresholds (model_version, class_id, minimum_confidence, configured_at)
SELECT 'two_stage_negative_rot4', class_id, 0.10, NOW()
FROM generate_series(0, 12) AS class_id;

CREATE TABLE observations (
    id UUID PRIMARY KEY,
    inspection_id UUID NOT NULL REFERENCES inspections(id) ON DELETE RESTRICT,
    source_detection_id UUID NOT NULL UNIQUE REFERENCES media_analysis_detections(id) ON DELETE RESTRICT,
    type VARCHAR(48) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'VIEWED', 'DISMISSED')),
    ai_zone VARCHAR(32) NOT NULL,
    zone_confidence DOUBLE PRECISION,
    zone_uncertain BOOLEAN NOT NULL,
    zone_model_version VARCHAR(128),
    user_corrected_zone VARCHAR(32),
    class_id INTEGER NOT NULL CHECK (class_id BETWEEN 0 AND 12),
    ai_label VARCHAR(64) NOT NULL,
    confidence DOUBLE PRECISION NOT NULL CHECK (confidence BETWEEN 0 AND 1),
    observation_min_confidence DOUBLE PRECISION NOT NULL CHECK (observation_min_confidence BETWEEN 0 AND 1),
    model_version VARCHAR(128) NOT NULL,
    template_version VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_observations_inspection
    ON observations (inspection_id, status, ai_zone, created_at);

CREATE TABLE observation_evidence (
    observation_id UUID NOT NULL REFERENCES observations(id) ON DELETE CASCADE,
    media_id UUID NOT NULL REFERENCES media(id) ON DELETE RESTRICT,
    detection_id UUID NOT NULL REFERENCES media_analysis_detections(id) ON DELETE RESTRICT,
    is_representative BOOLEAN NOT NULL,
    coordinate_system VARCHAR(24) NOT NULL CHECK (coordinate_system = 'PIXEL_XYXY'),
    image_width INTEGER NOT NULL CHECK (image_width > 0),
    image_height INTEGER NOT NULL CHECK (image_height > 0),
    bbox_left DOUBLE PRECISION NOT NULL CHECK (bbox_left >= 0),
    bbox_top DOUBLE PRECISION NOT NULL CHECK (bbox_top >= 0),
    bbox_right DOUBLE PRECISION NOT NULL,
    bbox_bottom DOUBLE PRECISION NOT NULL,
    confidence DOUBLE PRECISION NOT NULL CHECK (confidence BETWEEN 0 AND 1),
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (observation_id, media_id),
    CONSTRAINT ck_observation_evidence_box CHECK (
        bbox_right > bbox_left AND bbox_bottom > bbox_top
        AND bbox_right <= image_width AND bbox_bottom <= image_height
    )
);

CREATE INDEX idx_observation_evidence_media
    ON observation_evidence (media_id, confidence DESC);

CREATE TABLE reports (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL REFERENCES properties(id) ON DELETE RESTRICT,
    inspection_id UUID NOT NULL UNIQUE REFERENCES inspections(id) ON DELETE RESTRICT,
    status VARCHAR(32) NOT NULL CHECK (status IN (
        'WAITING_FOR_ANALYSIS', 'GENERATING', 'COMPLETED', 'PARTIAL_COMPLETED', 'FAILED'
    )),
    successful_media_count INTEGER NOT NULL CHECK (successful_media_count >= 0),
    failed_media_count INTEGER NOT NULL CHECK (failed_media_count >= 0),
    observation_count INTEGER NOT NULL CHECK (observation_count >= 0),
    reference_score INTEGER CHECK (reference_score BETWEEN 0 AND 100),
    score_policy_version VARCHAR(64),
    score_is_provisional BOOLEAN NOT NULL,
    failure_code VARCHAR(64),
    template_version VARCHAR(64) NOT NULL,
    generated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_reports_property
    ON reports (property_id, created_at DESC);
