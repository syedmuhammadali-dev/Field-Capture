-- PostgreSQL Schema for FieldCapture Deliveries Table
-- Section 15 of Architecture Requirements

CREATE TABLE IF NOT EXISTS deliveries (
    id VARCHAR(64) PRIMARY KEY,
    supplier_name VARCHAR(255) NOT NULL,
    po_number VARCHAR(100) NOT NULL,
    note TEXT,
    photo_url VARCHAR(500),
    idempotency_key VARCHAR(128) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_deliveries_idempotency UNIQUE (idempotency_key)
);

-- Index for speedy queries by PO and supplier
CREATE INDEX IF NOT EXISTS idx_deliveries_po_number ON deliveries(po_number);
CREATE INDEX IF NOT EXISTS idx_deliveries_supplier_name ON deliveries(supplier_name);
