CREATE TABLE inspections (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL REFERENCES properties(id) ON DELETE RESTRICT,
    owner_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    analysis_status VARCHAR(32) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_inspections_status
        CHECK (status IN ('IN_PROGRESS', 'ENDED', 'CANCELLED')),
    CONSTRAINT chk_inspections_analysis_status
        CHECK (analysis_status IN (
            'NOT_STARTED',
            'UPLOADING',
            'QUEUED',
            'ANALYZING',
            'PARTIAL_COMPLETED',
            'COMPLETED',
            'FAILED'
        )),
    CONSTRAINT chk_inspections_terminal_timestamp
        CHECK (
            (status = 'IN_PROGRESS' AND ended_at IS NULL AND cancelled_at IS NULL)
            OR (status = 'ENDED' AND ended_at IS NOT NULL AND cancelled_at IS NULL)
            OR (status = 'CANCELLED' AND ended_at IS NULL AND cancelled_at IS NOT NULL)
        )
);

CREATE INDEX idx_inspections_owner_property_created
    ON inspections (owner_id, property_id, created_at DESC, id DESC);
