-- Modüler monolit geçişi, Adım 1: kullanılmayan ortak tabloyu düşürür.
-- processed_event yalnız reporting-service kodunda okunup yazılır (ProcessedEventAdapter); bu serviste hiçbir
-- kod kullanmaz, tablo daima boştur.
DROP TABLE IF EXISTS processed_event;
