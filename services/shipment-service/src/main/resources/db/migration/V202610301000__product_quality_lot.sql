-- İplik (ürün), kalite sınıfı ve parti tanımları; partinin kalite kilidi (ön analiz Bölüm 4.1 ve 10, eksik listesi
-- 1.2 ve 1.3). Diğer ana veri gibi master-data servisi gelene kadar shipment_db içinde `md_` önekiyle durur.
-- Kayıt silinmez, pasifleştirilir. Kodlar çuvala ve etikete kopyalandığı için eklendikten sonra değişmez.

CREATE TABLE md_product (
    id         uuid        PRIMARY KEY,
    code       text        NOT NULL,
    name       text        NOT NULL,
    yarn_count text,
    active     boolean     NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL,
    created_by uuid        NOT NULL
);

COMMENT ON TABLE md_product IS 'İplik / ürün';
COMMENT ON COLUMN md_product.yarn_count IS 'İplik numarası, ör. Ne 30/1; serbest metin';

CREATE UNIQUE INDEX md_product_code_key ON md_product (upper(code));

CREATE TABLE md_quality_grade (
    id          uuid        PRIMARY KEY,
    code        text        NOT NULL,
    name        text        NOT NULL,
    label_color text        CHECK (label_color ~ '^#[0-9A-F]{6}$'),
    active      boolean     NOT NULL DEFAULT true,
    created_at  timestamptz NOT NULL,
    created_by  uuid        NOT NULL
);

COMMENT ON TABLE md_quality_grade IS 'Kalite sınıfı (A, B ...); kod çuvala kopyalanır';
COMMENT ON COLUMN md_quality_grade.label_color IS 'Etiket rengi, #RRGGBB; A/B ayrımı için (rapor Bölüm 10)';

CREATE UNIQUE INDEX md_quality_grade_code_key ON md_quality_grade (upper(code));

CREATE TABLE md_production_lot (
    id                uuid        PRIMARY KEY,
    site_id           uuid        NOT NULL REFERENCES md_site (id),
    lot_no            text        NOT NULL,
    product_id        uuid        NOT NULL REFERENCES md_product (id),
    quality_grade_id  uuid        NOT NULL REFERENCES md_quality_grade (id),
    production_date   date        NOT NULL,
    quality_locked_at timestamptz,
    quality_locked_by uuid,
    active            boolean     NOT NULL DEFAULT true,
    created_at        timestamptz NOT NULL,
    created_by        uuid        NOT NULL,
    CONSTRAINT md_production_lot_quality_lock_complete
        CHECK ((quality_locked_at IS NULL) = (quality_locked_by IS NULL))
);

COMMENT ON TABLE md_production_lot IS 'Parti; kalite kilitlendikten sonra yalnızca etkinlik durumu değişir (rapor Bölüm 10)';

CREATE UNIQUE INDEX md_production_lot_site_lot_no_key ON md_production_lot (site_id, upper(lot_no));
CREATE INDEX md_production_lot_product_idx ON md_production_lot (product_id);
CREATE INDEX md_production_lot_quality_grade_idx ON md_production_lot (quality_grade_id);

GRANT SELECT, INSERT, UPDATE ON md_product, md_quality_grade, md_production_lot TO ${app_role};

CREATE TRIGGER md_product_guard
    AFTER UPDATE OR DELETE ON md_product
    FOR EACH ROW EXECUTE FUNCTION guard_immutable_columns('name', 'yarn_count', 'active');

CREATE TRIGGER md_product_audit_local
    AFTER INSERT OR UPDATE OR DELETE ON md_product
    FOR EACH ROW EXECUTE FUNCTION audit_local_capture();

CREATE TRIGGER md_quality_grade_guard
    AFTER UPDATE OR DELETE ON md_quality_grade
    FOR EACH ROW EXECUTE FUNCTION guard_immutable_columns('name', 'label_color', 'active');

CREATE TRIGGER md_quality_grade_audit_local
    AFTER INSERT OR UPDATE OR DELETE ON md_quality_grade
    FOR EACH ROW EXECUTE FUNCTION audit_local_capture();

-- Kilitsiz partide lokasyon dışındaki tanım alanları ve kilit değişebilir; kimlik ve oluşturma bilgisi hiç değişmez.
CREATE TRIGGER md_production_lot_guard
    AFTER UPDATE OR DELETE ON md_production_lot
    FOR EACH ROW EXECUTE FUNCTION guard_immutable_columns(
        'lot_no', 'product_id', 'quality_grade_id', 'production_date', 'quality_locked_at', 'quality_locked_by',
        'active');

-- Rapor Bölüm 10: kilitli partinin kalitesi, ürünü, numarası ve kilidi geri alınamaz; yalnızca pasife alınabilir.
CREATE FUNCTION guard_md_lot_quality_lock()
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
            OR NEW.quality_locked_at IS DISTINCT FROM OLD.quality_locked_at
            OR NEW.quality_locked_by IS DISTINCT FROM OLD.quality_locked_by THEN
        RAISE EXCEPTION 'Lot quality is locked lotId=%', OLD.id USING ERRCODE = 'YS004';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER md_production_lot_quality_lock
    BEFORE UPDATE ON md_production_lot
    FOR EACH ROW EXECUTE FUNCTION guard_md_lot_quality_lock();

CREATE TRIGGER md_production_lot_audit_local
    AFTER INSERT OR UPDATE OR DELETE ON md_production_lot
    FOR EACH ROW EXECUTE FUNCTION audit_local_capture();
