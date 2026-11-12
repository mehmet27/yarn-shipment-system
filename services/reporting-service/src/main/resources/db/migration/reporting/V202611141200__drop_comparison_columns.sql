-- Çıkış–teslim karşılaştırması kaldırıldı (süreç analizi v2.0): bant, fark ve karşılaştırma zamanı artık tutulmuyor.
ALTER TABLE shipment_view
    DROP COLUMN IF EXISTS band,
    DROP COLUMN IF EXISTS diff_pct,
    DROP COLUMN IF EXISTS diff_kg,
    DROP COLUMN IF EXISTS compared_at;
