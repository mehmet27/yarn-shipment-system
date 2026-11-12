-- Kantar fişi okumaları (evidence-service TicketRead olayı). Elle girilen tartımın yerine geçmez; teslim
-- karşılaştırmasında fişle çapraz kontrol (K1–K5) için kullanılır. Olay tekrar gelirse ilk kayıt kalır.
CREATE TABLE ticket_reading (
    evidence_id   uuid           PRIMARY KEY,
    shipment_id   uuid           NOT NULL,
    evidence_type text           NOT NULL CHECK (evidence_type IN ('DISPATCH_SCALE', 'DELIVERY_SCALE')),
    status        text           NOT NULL CHECK (status IN ('READ', 'UNREADABLE')),
    gross_kg      numeric(12, 2),
    tare_kg       numeric(12, 2),
    net_kg        numeric(12, 2),
    plate         text,
    weighed_at    timestamptz,
    ticket_no     text,
    scale_name    text,
    problems      text[]         NOT NULL DEFAULT '{}',
    read_at       timestamptz    NOT NULL,
    created_at    timestamptz    NOT NULL,
    created_by    uuid           NOT NULL,
    CONSTRAINT ticket_reading_weights CHECK (
        (status = 'READ' AND gross_kg IS NOT NULL AND tare_kg IS NOT NULL AND net_kg = gross_kg - tare_kg
            AND tare_kg >= 0 AND net_kg > 0)
        OR (status = 'UNREADABLE' AND gross_kg IS NULL AND tare_kg IS NULL AND net_kg IS NULL))
);

COMMENT ON TABLE ticket_reading IS 'Kantar fişi okumaları; değiştirilemez';

CREATE INDEX ticket_reading_shipment_idx ON ticket_reading (shipment_id, evidence_type, read_at);
-- Tekrar fiş kontrolü (K5): fiş no ve kantar adı boşluk ve harf büyüklüğü yok sayılarak eşleşir.
CREATE INDEX ticket_reading_ticket_idx ON ticket_reading (upper(regexp_replace(ticket_no, '[[:space:]]', '', 'g')))
    WHERE ticket_no IS NOT NULL;

CREATE TRIGGER ticket_reading_append_only
    BEFORE UPDATE OR DELETE ON ticket_reading
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

GRANT SELECT, INSERT ON ticket_reading TO ${app_role};
