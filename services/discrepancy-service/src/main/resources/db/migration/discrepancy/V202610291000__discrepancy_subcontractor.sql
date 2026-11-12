-- Fasoncu kendi sevkiyatının uyuşmazlıklarını görür (yetki matrisi, DISCREPANCY_VIEW "kendi"; QA H-02). Fasoncu
-- kimliği teslim olayının bağlamından gelir. Önceden açılmış kayıtlarda boş kalır; bu kayıtlar yalnızca iç
-- kullanıcılara görünür.
ALTER TABLE discrepancy ADD COLUMN subcontractor_id uuid;

COMMENT ON COLUMN discrepancy.subcontractor_id IS 'Sevkiyatın fasoncusu; DeliveryReceived bağlamından, eski kayıtlarda boş';

CREATE INDEX discrepancy_subcontractor_status_idx ON discrepancy (subcontractor_id, status);
