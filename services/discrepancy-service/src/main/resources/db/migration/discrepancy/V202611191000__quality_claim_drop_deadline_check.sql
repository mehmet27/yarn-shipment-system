-- Kalite itirazı açmak için süre sınırı yoktur (ClaimTarget.isOpenAt her zaman true; iş birimi kararı, akış dokümanı
-- S18). deadline_at yalnızca bilgi amaçlı hedef tarihtir. İlk şemadaki opened_at <= deadline_at kısıtı bu kararla
-- çelişiyordu: hedef tarihten sonra açılan her itiraz veritabanında reddedilip 500 dönüyordu.
ALTER TABLE quality_claim DROP CONSTRAINT quality_claim_opened_before_deadline;
