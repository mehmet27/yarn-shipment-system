-- Hiçbir kodun okumadığı ya da yazmadığı tablolar ve kolonlar temizlenir: karşılaştırma girdisi, tolerans ve süre
-- kuralları, kanıt ve kantar fişi izleri, eski çuval bazlı itiraz bağları ile yolda süre uyarısı kolonları.
DROP TABLE IF EXISTS ticket_misread;
DROP TABLE IF EXISTS ticket_reading;
DROP TABLE IF EXISTS shipment_evidence;
DROP TABLE IF EXISTS comparison_input;
DROP TABLE IF EXISTS tolerance_rule;
DROP TABLE IF EXISTS time_limit_rule;
DROP TABLE IF EXISTS quality_claim_bag;

DROP INDEX IF EXISTS shipment_watch_in_transit_idx;
ALTER TABLE shipment_watch
    DROP COLUMN IF EXISTS arrived_at,
    DROP COLUMN IF EXISTS transit_alerted_at;
