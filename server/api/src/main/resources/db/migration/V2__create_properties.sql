CREATE TABLE properties (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL CHECK (btrim(name) <> ''),
    address_summary VARCHAR(500),
    deposit_amount BIGINT CHECK (deposit_amount >= 0),
    monthly_rent_amount BIGINT CHECK (monthly_rent_amount >= 0),
    maintenance_fee_amount BIGINT CHECK (maintenance_fee_amount >= 0),
    area_square_meters NUMERIC(12, 6) CHECK (area_square_meters >= 0.01),
    floor VARCHAR(100),
    broker_contact VARCHAR(300),
    note TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_properties_owner_created
    ON properties (owner_id, created_at DESC, id DESC);

CREATE TABLE property_options (
    property_id UUID NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    option_name VARCHAR(200) NOT NULL CHECK (btrim(option_name) <> ''),
    PRIMARY KEY (property_id, option_name)
);
