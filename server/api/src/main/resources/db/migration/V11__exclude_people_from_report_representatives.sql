ALTER TABLE media
    ADD COLUMN contains_person BOOLEAN;

ALTER TABLE reports
    DROP COLUMN reference_score,
    DROP COLUMN score_policy_version,
    DROP COLUMN score_is_provisional;

COMMENT ON COLUMN media.contains_person IS
    'Gemini privacy signal used to exclude photos containing people from report representative photos';
