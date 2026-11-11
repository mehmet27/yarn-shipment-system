-- İplik tedarikçisi (Şireci, Karesi, Teksmar ...) ve partiye isteğe bağlı bağlanması (süreç analizi v2, Bölüm 5.1 ve 9.2).
-- Kayıt silinmez, pasifleştirilir. Ad büyük/küçük harf duyarsız tekildir.

CREATE TABLE md_yarn_supplier (
    id         uuid        PRIMARY KEY,
    name       text        NOT NULL,
    active     boolean     NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL,
    created_by uuid        NOT NULL
);

COMMENT ON TABLE md_yarn_supplier IS 'İplik tedarikçisi';

CREATE UNIQUE INDEX md_yarn_supplier_name_key ON md_yarn_supplier (lower(name));

GRANT SELECT, INSERT, UPDATE ON md_yarn_supplier TO ${app_role};

CREATE TRIGGER md_yarn_supplier_guard
    AFTER UPDATE OR DELETE ON md_yarn_supplier
    FOR EACH ROW EXECUTE FUNCTION guard_immutable_columns('name', 'active');

CREATE TRIGGER md_yarn_supplier_audit_local
    AFTER INSERT OR UPDATE OR DELETE ON md_yarn_supplier
    FOR EACH ROW EXECUTE FUNCTION audit_local_capture();

ALTER TABLE md_production_lot ADD COLUMN yarn_supplier_id uuid REFERENCES md_yarn_supplier (id);

COMMENT ON COLUMN md_production_lot.yarn_supplier_id IS 'İplik tedarikçisi; isteğe bağlı, kalite kilidinden sonra değişmez';

CREATE INDEX md_production_lot_yarn_supplier_idx ON md_production_lot (yarn_supplier_id);

-- guard_immutable_columns argümanları (yetkili sütunlar) yeniden tanımlanır: tedarikçi de düzenlenebilir alan.
DROP TRIGGER md_production_lot_guard ON md_production_lot;
CREATE TRIGGER md_production_lot_guard
    AFTER UPDATE OR DELETE ON md_production_lot
    FOR EACH ROW EXECUTE FUNCTION guard_immutable_columns(
        'lot_no', 'product_id', 'quality_grade_id', 'production_date', 'quality_locked_at', 'quality_locked_by',
        'active', 'yarn_supplier_id');

-- Kilitli partide tedarikçi de değişemez (kalite kilidi trigger'ı genişletilir).
CREATE OR REPLACE FUNCTION guard_md_lot_quality_lock()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.quality_locked_at IS NULL THEN
        RETURN NEW;
    END IF;
    IF NEW.quality_grade_id IS DISTINCT FROM OLD.quality_grade_id
            OR NEW.product_id IS DISTINCT FROM OLD.product_id
            OR NEW.lot_no IS DISTINCT FROM OLD.lot_no
            OR NEW.production_date IS DISTINCT FROM OLD.production_date
            OR NEW.yarn_supplier_id IS DISTINCT FROM OLD.yarn_supplier_id
            OR NEW.quality_locked_at IS DISTINCT FROM OLD.quality_locked_at
            OR NEW.quality_locked_by IS DISTINCT FROM OLD.quality_locked_by THEN
        RAISE EXCEPTION 'Lot quality is locked lotId=%', OLD.id USING ERRCODE = 'YS004';
    END IF;
    RETURN NEW;
END;
$$;
