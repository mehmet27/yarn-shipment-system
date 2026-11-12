-- Kantar fişi "doğrulanamadı" (tasarım K0): kilo üç denemede okunamadı, kullanıcı gerekçe seçip devam etti.
-- Son denemenin okunamayan okumasına bağlıdır; her okuma için tek kayıt, kayıt değişmez.
CREATE TABLE ticket_unverified (
    evidence_id   uuid        PRIMARY KEY REFERENCES ticket_reading (evidence_id),
    shipment_id   uuid        NOT NULL,
    evidence_type text        NOT NULL CHECK (evidence_type IN ('DISPATCH_SCALE', 'DELIVERY_SCALE')),
    reason        text        NOT NULL CHECK (reason IN ('PRINTER_BROKEN', 'TICKET_DAMAGED', 'NO_TICKET', 'OTHER')),
    declared_at   timestamptz NOT NULL,
    created_at    timestamptz NOT NULL,
    created_by    uuid        NOT NULL
);

COMMENT ON TABLE ticket_unverified IS 'Okunamayan fişle gerekçeyle devam edilen tartımlar; uyuşmazlık açar';

CREATE INDEX ticket_unverified_shipment_idx ON ticket_unverified (shipment_id);

GRANT SELECT, INSERT ON ticket_unverified TO ${app_role};
