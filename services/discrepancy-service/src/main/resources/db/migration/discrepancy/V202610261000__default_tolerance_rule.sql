-- Genel tolerans kuralı (ön analiz raporu, Bölüm 9.2 ve karar #6): Yeşil ≤ %0,5, Sarı ≤ %1,5, üstü Kırmızı.
-- Pilotta kalibre edilir; yeni değer eski kuralın valid_to'su kapatılıp yeni satırla girilir.
INSERT INTO tolerance_rule (id, product_id, green_max_pct, yellow_max_pct, valid_from, created_at, created_by)
VALUES ('0192f0a0-0000-7000-8000-00000000c001', NULL, 0.50, 1.50, '2026-01-01 00:00:00+00', now(),
        '00000000-0000-0000-0000-000000000000');
