-- Kullanıcı açma ve şifre belirleme artık SMS/e-posta doğrulaması kullanmaz: ilk şifreyi yönetici yazar.
-- SMS kodu özeti ve yerel SMS gelen kutusu tabloları kaldırılır.

DROP TABLE IF EXISTS md_password_reset_code;
DROP TABLE IF EXISTS sms_dev_outbox;
