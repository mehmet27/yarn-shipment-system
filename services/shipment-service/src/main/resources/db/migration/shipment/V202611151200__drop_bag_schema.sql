-- Çuval etiketi ve çuval servisi (bag-service) kaldırıldı; sevkiyat çuvalları sıra no + kg olarak shipment_bag'de tutulur.
-- bag şemasına başka şemadan yabancı anahtar yoktur; tüm tablolar, fonksiyonlar ve tipler birlikte silinir.
DROP SCHEMA IF EXISTS bag CASCADE;
