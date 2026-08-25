-- AI 공간 분류의 MVP 지원 범위를 주방, 거실·방, 화장실로 제한한다.
-- 과거 계약의 현관·공용 및 창틀·환기 값은 삭제하지 않고 UNKNOWN으로 안전하게 정규화한다.
UPDATE media
SET zone = 'UNKNOWN'
WHERE zone IN ('ENTRANCE_COMMON', 'WINDOW_VENTILATION');

UPDATE media
SET ai_zone = 'UNKNOWN',
    zone_uncertain = TRUE
WHERE ai_zone IN ('ENTRANCE_COMMON', 'WINDOW_VENTILATION');

UPDATE media
SET user_corrected_zone = 'UNKNOWN'
WHERE user_corrected_zone IN ('ENTRANCE_COMMON', 'WINDOW_VENTILATION');

UPDATE observations
SET ai_zone = 'UNKNOWN',
    zone_uncertain = TRUE
WHERE ai_zone IN ('ENTRANCE_COMMON', 'WINDOW_VENTILATION');

UPDATE observations
SET user_corrected_zone = 'UNKNOWN'
WHERE user_corrected_zone IN ('ENTRANCE_COMMON', 'WINDOW_VENTILATION');

ALTER TABLE media
    DROP CONSTRAINT IF EXISTS ck_media_zone,
    DROP CONSTRAINT IF EXISTS ck_media_ai_zone,
    DROP CONSTRAINT IF EXISTS ck_media_user_corrected_zone;

ALTER TABLE media
    ADD CONSTRAINT ck_media_zone
        CHECK (zone IS NULL OR zone IN ('KITCHEN', 'LIVING_ROOM', 'BATHROOM', 'UNKNOWN')),
    ADD CONSTRAINT ck_media_ai_zone
        CHECK (ai_zone IS NULL OR ai_zone IN ('KITCHEN', 'LIVING_ROOM', 'BATHROOM', 'UNKNOWN')),
    ADD CONSTRAINT ck_media_user_corrected_zone
        CHECK (user_corrected_zone IS NULL OR user_corrected_zone IN ('KITCHEN', 'LIVING_ROOM', 'BATHROOM', 'UNKNOWN'));

ALTER TABLE observations
    ADD CONSTRAINT ck_observations_ai_zone
        CHECK (ai_zone IN ('KITCHEN', 'LIVING_ROOM', 'BATHROOM', 'UNKNOWN')),
    ADD CONSTRAINT ck_observations_user_corrected_zone
        CHECK (user_corrected_zone IS NULL OR user_corrected_zone IN ('KITCHEN', 'LIVING_ROOM', 'BATHROOM', 'UNKNOWN'));
