-- Yanlış üretilen çuvalın iptali: kim, ne zaman, neden. Çuval kaydı silinmez; durumu VOID olur. Kayıt eklenir, değişmez.

CREATE TABLE bag_void (
    id         uuid        PRIMARY KEY,
    bag_id     uuid        NOT NULL UNIQUE REFERENCES bag (id),
    reason     text        NOT NULL CHECK (length(btrim(reason)) > 0),
    voided_at  timestamptz NOT NULL,
    voided_by  uuid        NOT NULL,
    created_at timestamptz NOT NULL,
    created_by uuid        NOT NULL,
    updated_at timestamptz,
    updated_by uuid,
    version    integer     NOT NULL DEFAULT 0
);

COMMENT ON TABLE bag_void IS 'Çuval iptali; gerekçe zorunlu, yalnızca depodaki (CREATED) çuval iptal edilir';

CREATE TRIGGER bag_void_append_only
    BEFORE UPDATE OR DELETE ON bag_void
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

GRANT SELECT, INSERT ON bag_void TO ${app_role};
