-- reporting artık olay tüketmez: okuma modelleri canlı SQL görünümüdür ve olaylar süreç içi iletilir. Kimse
-- processed_event'e yazmıyor ya da okumuyor (idempotent tüketici tablosu kalktı).
DROP TABLE IF EXISTS processed_event;
