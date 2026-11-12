-- Kafka ve transactional outbox kalktı: modüller arası olaylar aynı süreçte, üreten işlemle birlikte iletilir
-- (DomainEventPublisher). outbox_event tablosu artık yazılmaz ve okunmaz.
--
-- Güvenlik: yayımlanmamış (published_at boş) satır varsa tablo düşürülmez. Eski sürümün yayıncısı bu satırları Kafka'ya
-- taşıyıp tüketicilerin işlemesini beklemeden geçiş yapılırsa olaylar sessizce kaybolurdu (DEPLOY.md, "Tek uygulamaya
-- geçiş"). Satır kalmışsa önce eski sürümü çalıştırıp boşalmasını bekleyin ya da kaybı kabul ediyorsanız satırları elle silin.
DO $$
DECLARE
    pending bigint;
BEGIN
    SELECT count(*) INTO pending FROM outbox_event WHERE published_at IS NULL;
    IF pending > 0 THEN
        RAISE EXCEPTION 'outbox_event has % unpublished rows; drain it with the previous release before upgrading', pending;
    END IF;
END
$$;

DROP TABLE outbox_event;
