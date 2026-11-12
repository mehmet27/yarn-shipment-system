-- Uyuşmazlık türü yalnızca kalite itirazıdır (QUALITY); miktar, çuval, mühür, belge, fotoğraf ve süre türleri kalktı.
-- Eski kayıtlar (append-only geçmişe bağlı) silinemediği için kısıt yalnızca yeni satırlara uygulanır.
ALTER TABLE discrepancy DROP CONSTRAINT discrepancy_discrepancy_type_check;
ALTER TABLE discrepancy
    ADD CONSTRAINT discrepancy_discrepancy_type_check CHECK (discrepancy_type IN ('QUALITY')) NOT VALID;
COMMENT ON COLUMN discrepancy.discrepancy_type IS 'Tasarımdaki type kolonu (yalnızca QUALITY)';
