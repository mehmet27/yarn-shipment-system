-- Sevkiyat dosyasındaki "Düzeltmeler" özelliği kaldırıldı: izin rollerden silinir, düzeltme kayıtları tablosu atılır.
-- Eski veri önemli değil. Tartım tablosundaki superseded_by sütunu çıkış tartısı yeniden girilince de kullanıldığı için kalır.
DELETE FROM role_permission WHERE permission = 'CORRECTION_CREATE';

DROP TABLE correction_record;
DROP FUNCTION guard_correction_decided_once();
