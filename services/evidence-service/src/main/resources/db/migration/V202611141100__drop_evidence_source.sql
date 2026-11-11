-- Galeri/kamera ayrımı kaldırıldı (süreç analizi v2.0): kaynak sütunu artık tutulmuyor.
ALTER TABLE evidence_file DROP COLUMN IF EXISTS source;
ALTER TABLE upload_intent DROP COLUMN IF EXISTS source;
