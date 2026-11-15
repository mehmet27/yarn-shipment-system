-- Sevkiyat tarihi: müdür sevkiyatı açarken girer; depo bu tarih gelmeden çuval atayamaz ve çıkış tartımı giremez.
-- Uygulama tarihi her zaman kendisi yazar; varsayılan (Türkiye saatiyle bugün) yalnızca doğrudan SQL ile eklenen
-- örnek/aktarım kayıtları içindir. Eski kayıtlar oluşturulma gününü alır.
ALTER TABLE shipment ADD COLUMN shipment_date date NOT NULL DEFAULT (timezone('Europe/Istanbul', now())::date);
UPDATE shipment SET shipment_date = (created_at AT TIME ZONE 'Europe/Istanbul')::date;
