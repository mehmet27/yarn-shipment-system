-- Modüler monolit geçişi, Adım 1: kullanılmayan ortak tabloları düşürür.
--  * audit_local: bu şemada hiçbir tabloya audit_local_capture() tetikleyicisi bağlı değil, tablo daima boş.
--  * outbox_event: reporting olay yayınlamaz (yarnshipment.outbox.topic verilmediği için yayıncı kapalı).
-- processed_event kalır: ApplyReadModelChangeService tüketici tekrarını bununla önler.
DROP FUNCTION IF EXISTS audit_local_capture();
DROP TABLE IF EXISTS audit_local;
DROP TABLE IF EXISTS outbox_event;
