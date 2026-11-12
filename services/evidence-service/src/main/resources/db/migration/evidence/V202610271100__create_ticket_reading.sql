-- Kantar fişi okuması: görüntü modelinin fişten okuduğu alanlar ve sunucunun güven kurallarının sonucu.
-- Her kanıt için tek kayıt; yeniden çekim yeni kanıt ve yeni okumadır, kayıt değişmez.
CREATE TABLE ticket_reading (
    evidence_id    uuid          PRIMARY KEY,
    shipment_id    uuid          NOT NULL,
    evidence_type  text          NOT NULL CHECK (evidence_type IN ('DISPATCH_SCALE', 'DELIVERY_SCALE')),
    status         text          NOT NULL CHECK (status IN ('READ', 'UNREADABLE')),
    gross_kg       numeric(12, 2),
    tare_kg        numeric(12, 2),
    net_kg         numeric(12, 2),
    plate          text,
    weighed_at     timestamptz,
    ticket_no      text,
    scale_name     text,
    problems       text[]        NOT NULL DEFAULT '{}',
    model          text          NOT NULL,
    prompt_version text          NOT NULL,
    raw_response   jsonb         NOT NULL,
    read_at        timestamptz   NOT NULL,
    created_at     timestamptz   NOT NULL,
    created_by     uuid          NOT NULL,
    CONSTRAINT ticket_reading_weights CHECK (
        (status = 'READ' AND gross_kg IS NOT NULL AND tare_kg IS NOT NULL AND net_kg = gross_kg - tare_kg
            AND tare_kg >= 0 AND net_kg > 0)
        OR (status = 'UNREADABLE' AND gross_kg IS NULL AND tare_kg IS NULL AND net_kg IS NULL))
);

COMMENT ON TABLE ticket_reading IS 'Kantar fişi okumaları; kayıt değişmez, okunamayan fiş yeniden çekilir';
COMMENT ON COLUMN ticket_reading.net_kg IS 'Brüt eksi dara; fişte basılı net yalnızca ham cevapta durur';
COMMENT ON COLUMN ticket_reading.raw_response IS 'Modelin ham cevabı; isabet incelemesi ve denetim için';

CREATE INDEX ticket_reading_shipment_idx ON ticket_reading (shipment_id, evidence_type);

GRANT SELECT, INSERT ON ticket_reading TO ${app_role};
