CREATE TABLE shipments (
    id                       BIGSERIAL PRIMARY KEY,
    tracking_number          VARCHAR(30)  NOT NULL,
    sender_name              VARCHAR(100) NOT NULL,
    receiver_name            VARCHAR(100) NOT NULL,
    origin                   VARCHAR(100) NOT NULL,
    destination              VARCHAR(100) NOT NULL,
    carrier                  VARCHAR(100) NOT NULL,
    status                   VARCHAR(20)  NOT NULL,
    expected_delivery_date   DATE         NOT NULL,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_shipments_tracking_number UNIQUE (tracking_number),
    CONSTRAINT ck_shipments_status CHECK (status IN
        ('CREATED', 'PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED'))
);

CREATE INDEX idx_shipments_status ON shipments (status);
