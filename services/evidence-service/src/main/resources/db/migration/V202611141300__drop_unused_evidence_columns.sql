-- Hiçbir kodun yazmadığı ya da okumadığı kolonlar: uyuşmazlık bağı, filigranlı kopya anahtarı, cihaz bilgisi ve konum.
CREATE OR REPLACE FUNCTION guard_evidence_invalidation()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.status = 'INVALIDATED' AND (NEW.status, NEW.invalidated_reason) IS DISTINCT FROM
            (OLD.status, OLD.invalidated_reason) THEN
        RAISE EXCEPTION 'Evidence already invalidated evidenceId=%', OLD.id USING ERRCODE = 'YS002';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS evidence_file_guard ON evidence_file;
CREATE TRIGGER evidence_file_guard
    AFTER UPDATE OR DELETE ON evidence_file
    FOR EACH ROW EXECUTE FUNCTION guard_immutable_columns('status', 'invalidated_reason');

DROP INDEX IF EXISTS evidence_file_discrepancy_idx;
ALTER TABLE evidence_file DROP CONSTRAINT IF EXISTS evidence_file_geo_requires_consent;
ALTER TABLE evidence_file
    DROP COLUMN IF EXISTS discrepancy_id,
    DROP COLUMN IF EXISTS watermarked_key,
    DROP COLUMN IF EXISTS device_info,
    DROP COLUMN IF EXISTS geo_lat,
    DROP COLUMN IF EXISTS geo_lon,
    DROP COLUMN IF EXISTS geo_consent;
