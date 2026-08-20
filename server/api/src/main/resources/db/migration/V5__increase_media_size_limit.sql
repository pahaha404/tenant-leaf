ALTER TABLE media
    DROP CONSTRAINT IF EXISTS media_declared_file_size_check;

ALTER TABLE media
    ADD CONSTRAINT media_declared_file_size_check
    CHECK (declared_file_size BETWEEN 1 AND 2097152);
