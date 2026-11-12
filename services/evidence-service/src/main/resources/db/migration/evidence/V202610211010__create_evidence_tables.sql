-- evidence_db: fotoğraf ve belge üst verisi (veritabanı tasarımı, "evidence_db").
-- Dosyanın kendisi MinIO'dadır (evidence/{yıl}/{ay}/{shipment_id}/{evidence_id}). Kayıt silinmez;
-- yanlış yükleme INVALIDATED olur (DOM-17).

CREATE TABLE evidence_file (
    id                 uuid        NOT NULL,
    created_at         timestamptz NOT NULL,
    shipment_id        uuid        NOT NULL,
    discrepancy_id     uuid,
    evidence_type      text        NOT NULL CHECK (evidence_type IN (
        'DISPATCH_SCALE', 'BAG_OVERVIEW', 'BAG_LABEL', 'VEHICLE_PLATE', 'SEAL', 'DELIVERY_SCALE',
        'DELIVERY_BAGS', 'QUALITY_CLAIM', 'SIGNED_DELIVERY_DOC', 'LAB_RESULT')),
    storage_key        text        NOT NULL,
    watermarked_key    text,
    mime_type          text        NOT NULL,
    size_bytes         bigint      NOT NULL CHECK (size_bytes > 0),
    sha256             char(64)    NOT NULL CHECK (sha256 ~ '^[0-9a-f]{64}$'),
    source             text        NOT NULL CHECK (source IN ('CAMERA', 'GALLERY')),
    captured_at        timestamptz,
    uploaded_at        timestamptz NOT NULL,
    uploaded_by        uuid        NOT NULL,
    device_info        jsonb,
    geo_lat            numeric(9, 6) CHECK (geo_lat BETWEEN -90 AND 90),
    geo_lon            numeric(9, 6) CHECK (geo_lon BETWEEN -180 AND 180),
    geo_consent        boolean     NOT NULL DEFAULT false,
    status             text        NOT NULL CHECK (status IN ('VALID', 'INVALIDATED')),
    invalidated_reason text,
    created_by         uuid        NOT NULL,
    updated_at         timestamptz,
    updated_by         uuid,
    version            integer     NOT NULL DEFAULT 0,
    PRIMARY KEY (id, created_at),
    CONSTRAINT evidence_file_geo_requires_consent
        CHECK (geo_consent OR (geo_lat IS NULL AND geo_lon IS NULL)),
    CONSTRAINT evidence_file_invalidation_reason
        CHECK (status = 'VALID' OR length(btrim(coalesce(invalidated_reason, ''))) > 0)
) PARTITION BY RANGE (created_at);

COMMENT ON TABLE evidence_file IS 'Kanıt dosyası üst verisi; silinmez, yalnızca INVALIDATED olur';
COMMENT ON COLUMN evidence_file.sha256 IS 'Sunucunun nesne deposundan okuyup hesapladığı hash (SEC-09); bilerek unique değil';
COMMENT ON COLUMN evidence_file.captured_at IS 'Cihazın bildirdiği çekim zamanı; karar kurallarında kullanılmaz (DOM-11)';
COMMENT ON COLUMN evidence_file.uploaded_at IS 'Sunucunun alış zamanı';

CREATE INDEX evidence_file_shipment_type_idx ON evidence_file (shipment_id, evidence_type);
CREATE INDEX evidence_file_sha256_idx ON evidence_file (sha256);
CREATE INDEX evidence_file_discrepancy_idx ON evidence_file (discrepancy_id) WHERE discrepancy_id IS NOT NULL;

-- Yükleme sonrası yalnızca filigranlı kopya eklenebilir ve kayıt bir kez geçersiz sayılabilir.
CREATE TRIGGER evidence_file_guard
    AFTER UPDATE OR DELETE ON evidence_file
    FOR EACH ROW EXECUTE FUNCTION guard_immutable_columns('status', 'invalidated_reason', 'watermarked_key');

CREATE FUNCTION guard_evidence_invalidation()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.status = 'INVALIDATED' AND (NEW.status, NEW.invalidated_reason) IS DISTINCT FROM
            (OLD.status, OLD.invalidated_reason) THEN
        RAISE EXCEPTION 'Evidence already invalidated evidenceId=%', OLD.id USING ERRCODE = 'YS002';
    END IF;
    IF OLD.watermarked_key IS NOT NULL AND NEW.watermarked_key IS DISTINCT FROM OLD.watermarked_key THEN
        RAISE EXCEPTION 'Watermarked copy cannot change evidenceId=%', OLD.id USING ERRCODE = 'YS002';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER evidence_file_invalidation
    BEFORE UPDATE ON evidence_file
    FOR EACH ROW EXECUTE FUNCTION guard_evidence_invalidation();

CREATE TRIGGER evidence_file_audit_local
    AFTER INSERT OR UPDATE OR DELETE ON evidence_file
    FOR EACH ROW EXECUTE FUNCTION audit_local_capture();

-- evidence_file partition'lı olduğu için (PK id + created_at) buradan FK verilmez; kimlikler
-- uygulama tarafından aynı transaction'da doğrulanır.
CREATE TABLE evidence_duplicate (
    id              uuid        PRIMARY KEY,
    evidence_id     uuid        NOT NULL,
    duplicate_of_id uuid        NOT NULL,
    detected_at     timestamptz NOT NULL,
    created_at      timestamptz NOT NULL,
    created_by      uuid        NOT NULL,
    UNIQUE (evidence_id, duplicate_of_id),
    CONSTRAINT evidence_duplicate_not_self CHECK (evidence_id <> duplicate_of_id)
);

COMMENT ON TABLE evidence_duplicate IS 'Aynı hash başka sevkiyatta görüldü; PHOTO uyuşmazlığını tetikler';

CREATE TRIGGER evidence_duplicate_append_only
    BEFORE UPDATE OR DELETE ON evidence_duplicate
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

GRANT SELECT, INSERT, UPDATE ON evidence_file TO ${app_role};
GRANT SELECT, INSERT ON evidence_duplicate TO ${app_role};

SELECT create_monthly_partitions('evidence_file', date '2026-10-01', 15);
