-- "AI yanıldı" işaretleri: inceleyen kişi uyuşmazlık ekranında fiş okumasının yanlış olduğunu işaretler.
-- Okuma başına tek kayıt; isabet ölçümüne girer, kayıt değişmez.
CREATE TABLE ticket_misread (
    evidence_id    uuid        PRIMARY KEY REFERENCES ticket_reading (evidence_id),
    discrepancy_id uuid        NOT NULL REFERENCES discrepancy (id),
    note           text        CHECK (note IS NULL OR length(btrim(note)) > 0),
    marked_at      timestamptz NOT NULL,
    created_at     timestamptz NOT NULL,
    created_by     uuid        NOT NULL
);

COMMENT ON TABLE ticket_misread IS 'Yanlış okunduğu işaretlenen kantar fişleri; değiştirilemez';

CREATE INDEX ticket_misread_discrepancy_idx ON ticket_misread (discrepancy_id);

CREATE TRIGGER ticket_misread_append_only
    BEFORE UPDATE OR DELETE ON ticket_misread
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

GRANT SELECT, INSERT ON ticket_misread TO ${app_role};
