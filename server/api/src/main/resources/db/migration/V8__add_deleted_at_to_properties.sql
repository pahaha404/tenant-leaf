ALTER TABLE properties ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_properties_owner_deleted_created
    ON properties (owner_id, deleted_at, created_at DESC, id DESC);
