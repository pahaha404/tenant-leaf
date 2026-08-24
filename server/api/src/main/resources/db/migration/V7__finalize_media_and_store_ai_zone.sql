ALTER TABLE inspections
    ADD COLUMN media_finalized_at TIMESTAMPTZ,
    ADD COLUMN expected_media_count INTEGER;

ALTER TABLE inspections
    ADD CONSTRAINT ck_inspections_expected_media_count
        CHECK (expected_media_count IS NULL OR expected_media_count >= 0),
    ADD CONSTRAINT ck_inspections_media_finalization
        CHECK (
            (media_finalized_at IS NULL AND expected_media_count IS NULL)
            OR (media_finalized_at IS NOT NULL AND expected_media_count IS NOT NULL)
        );

ALTER TABLE media
    ALTER COLUMN zone DROP NOT NULL,
    ADD COLUMN ai_zone VARCHAR(32),
    ADD COLUMN zone_confidence DOUBLE PRECISION,
    ADD COLUMN zone_uncertain BOOLEAN,
    ADD COLUMN zone_model_version VARCHAR(128),
    ADD COLUMN user_corrected_zone VARCHAR(32),
    ADD COLUMN corrected_at TIMESTAMPTZ;

ALTER TABLE media
    ADD CONSTRAINT ck_media_zone_confidence
        CHECK (zone_confidence IS NULL OR zone_confidence BETWEEN 0 AND 1),
    ADD CONSTRAINT ck_media_ai_zone
        CHECK (ai_zone IS NULL OR ai_zone IN (
            'ENTRANCE_COMMON', 'KITCHEN', 'WINDOW_VENTILATION',
            'LIVING_ROOM', 'BATHROOM', 'UNKNOWN'
        )),
    ADD CONSTRAINT ck_media_user_corrected_zone
        CHECK (user_corrected_zone IS NULL OR user_corrected_zone IN (
            'ENTRANCE_COMMON', 'KITCHEN', 'WINDOW_VENTILATION',
            'LIVING_ROOM', 'BATHROOM', 'UNKNOWN'
        )),
    ADD CONSTRAINT ck_media_zone_correction
        CHECK (
            (user_corrected_zone IS NULL AND corrected_at IS NULL)
            OR (user_corrected_zone IS NOT NULL AND corrected_at IS NOT NULL)
        );
