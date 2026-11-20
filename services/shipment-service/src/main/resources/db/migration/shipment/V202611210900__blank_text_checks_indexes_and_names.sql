-- Veritabanı incelemesinin güvenli iyileştirmeleri: veri silmez, kolon ya da tip değiştirmez.
--
-- 1) Zorunlu ad kolonları boş (yalnızca boşluk) olamaz. Şemanın geri kalanı bu kuralı CHECK ile zorlar
--    (yarn_type, fabric_type, invoice_no ...); tanım tablolarında ve sevkiyatın numara / fasoncu adı kopyasında eksikti.
--    Kısıtlar NOT VALID eklenir (yeni ve güncellenen satırlar hemen denetlenir, tablo kilidi kısa sürer), ardından
--    doğrulanır. Eski bir kurulumda boş adlı satır varsa doğrulama uyarı verip geçer; göç durmaz, kısıt yine de
--    yeni satırlara uygulanır.
ALTER TABLE md_site ADD CONSTRAINT md_site_name_not_blank CHECK (length(btrim(name)) > 0) NOT VALID;
ALTER TABLE md_subcontractor
    ADD CONSTRAINT md_subcontractor_title_not_blank CHECK (length(btrim(title)) > 0) NOT VALID;
ALTER TABLE md_driver ADD CONSTRAINT md_driver_full_name_not_blank CHECK (length(btrim(full_name)) > 0) NOT VALID;
ALTER TABLE md_vehicle ADD CONSTRAINT md_vehicle_plate_no_not_blank CHECK (length(btrim(plate_no)) > 0) NOT VALID;
ALTER TABLE md_product
    ADD CONSTRAINT md_product_code_not_blank CHECK (length(btrim(code)) > 0) NOT VALID,
    ADD CONSTRAINT md_product_name_not_blank CHECK (length(btrim(name)) > 0) NOT VALID;
ALTER TABLE md_quality_grade
    ADD CONSTRAINT md_quality_grade_code_not_blank CHECK (length(btrim(code)) > 0) NOT VALID,
    ADD CONSTRAINT md_quality_grade_name_not_blank CHECK (length(btrim(name)) > 0) NOT VALID;
ALTER TABLE md_yarn_supplier
    ADD CONSTRAINT md_yarn_supplier_name_not_blank CHECK (length(btrim(name)) > 0) NOT VALID;
ALTER TABLE md_production_lot
    ADD CONSTRAINT md_production_lot_lot_no_not_blank CHECK (length(btrim(lot_no)) > 0) NOT VALID;
ALTER TABLE shipment
    ADD CONSTRAINT shipment_shipment_no_not_blank CHECK (length(btrim(shipment_no)) > 0) NOT VALID,
    ADD CONSTRAINT shipment_subcontractor_title_not_blank CHECK (length(btrim(subcontractor_title)) > 0) NOT VALID;

DO $$
DECLARE
    pending record;
BEGIN
    FOR pending IN
        SELECT c.conrelid::regclass AS table_name, c.conname
        FROM pg_constraint c
        WHERE c.connamespace = current_schema()::regnamespace
          AND c.contype = 'c'
          AND NOT c.convalidated
          AND c.conname LIKE '%\_not\_blank'
    LOOP
        BEGIN
            EXECUTE format('ALTER TABLE %s VALIDATE CONSTRAINT %I', pending.table_name, pending.conname);
        EXCEPTION WHEN check_violation THEN
            RAISE WARNING '% kısıtı eski satırlar yüzünden doğrulanamadı; yalnızca yeni satırlara uygulanıyor',
                pending.conname;
        END;
    END LOOP;
END
$$;

-- 2) Eksik indeksler (sorgusu olan ama indeksi olmayan iki yer).
--    Sevkiyatın merkez depodan düşen hareketleri shipment_id ile okunur (çıkıştan sonraki merkez stok eksiği denetimi).
CREATE INDEX central_stock_movement_shipment_idx ON central_stock_movement (shipment_id)
    WHERE shipment_id IS NOT NULL;
--    Faturalama ekranındaki "Faturalananlar" listesi fatura tarihi aralığıyla süzülür ve sıralanır.
CREATE INDEX fire_invoice_invoiced_on_idx ON fire_invoice (invoiced_on);

-- 3) İsim tutarlılığı: fabric_receipt tablosu fabric_lot oldu (V202611161120) ama üç kısıt eski adla kaldı; app_user'ın
--    tekil indeksi de şemadaki <tablo>_<kolon>_key kalıbının dışındaydı. Yalnızca ad değişir, kural aynıdır.
ALTER TABLE fabric_lot RENAME CONSTRAINT fabric_receipt_fabric_type_check TO fabric_lot_fabric_type_check;
ALTER TABLE fabric_lot RENAME CONSTRAINT fabric_receipt_net_kg_check TO fabric_lot_net_kg_check;
ALTER TABLE fabric_lot RENAME CONSTRAINT fabric_receipt_shipment_id_fkey TO fabric_lot_shipment_id_fkey;
ALTER INDEX ux_app_user_username RENAME TO app_user_username_key;
