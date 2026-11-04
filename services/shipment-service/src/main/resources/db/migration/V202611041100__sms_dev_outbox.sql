-- Yalnızca yerel/test ortamı için SMS "gelen kutusu": sahte gönderici (yarnshipment.sms.provider=log) VE local profil
-- açıkken gönderilen mesajın düz metni (kod dahil) buraya yazılır, böylece test sırasında kod SELECT ile görülür.
-- Tablo her ortamda oluşur ama yalnızca LocalSmsDevOutbox yazar; canlıda (provider=none/gerçek) hiç satır olmaz.
-- md_password_reset_code'a düz kod kolonu EKLENMEZ: asıl tablo her ortamda yalnızca tuzlu özet taşır.
-- Gizli veri taşıdığı için denetim tetikleyicisi yoktur.

CREATE TABLE sms_dev_outbox (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    phone      text        NOT NULL,
    message    text        NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

COMMENT ON TABLE sms_dev_outbox IS 'YALNIZCA yerel/test: sahte SMS göndericinin düz metin mesajları (kod dahil); canlıda boş kalır';

GRANT SELECT, INSERT, DELETE ON sms_dev_outbox TO ${app_role};
