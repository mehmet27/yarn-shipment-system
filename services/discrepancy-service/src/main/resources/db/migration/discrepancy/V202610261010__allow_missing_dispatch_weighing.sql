-- Çıkış tartısı olmadan yola çıkmış sevkiyat da karşılaştırılır; eksik tartı QUANTITY_DISPATCH_MISSING
-- (Kırmızı) uyuşmazlığı olarak açılır, girdi kaydı yine yazılır.
ALTER TABLE comparison_input ALTER COLUMN dispatch_net_kg DROP NOT NULL;
