-- Standart dara tanımı ve çıkış tartımındaki dara sapması uyarısı kaldırıldı: dara artık boş araç tartısıdır ve
-- araçtan araca değişir, çuval başı standartla kıyaslanması anlamsız uyarı üretiyordu.
ALTER TABLE weighing DROP CONSTRAINT weighing_tare_basis_complete;
ALTER TABLE weighing
    DROP COLUMN expected_tare_kg,
    DROP COLUMN tare_tolerance_pct;

DROP TABLE md_tare_standard;
