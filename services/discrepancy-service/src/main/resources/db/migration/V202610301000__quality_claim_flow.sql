-- Kalite itirazı ve numune/lab akışı (ön analiz, Bölüm 10 ve karar #7).
--
-- İtiraz teslim karşılaştırmasından sonra açılır. Lokasyon, fasoncu, sevkiyat no ve teslimde okutulan anahtarlar
-- karşılaştırma girdisinden okunur; bu alanlar daha önce yalnızca uyuşmazlık satırlarında tutuluyordu ve temiz
-- sevkiyatın uyuşmazlığı olmadığı için girdiye eklenir. Önceden yazılmış girdilerde boş kalır; o sevkiyatlara
-- itiraz açılamaz.
ALTER TABLE comparison_input
    ADD COLUMN site_id          uuid,
    ADD COLUMN subcontractor_id uuid,
    ADD COLUMN shipment_no      text;

COMMENT ON COLUMN comparison_input.site_id IS 'Çıkış lokasyonu; itiraz kapsam kontrolü için, eski girdilerde boş';
COMMENT ON COLUMN comparison_input.subcontractor_id IS 'Sevkiyatın fasoncusu; eski girdilerde boş';

-- discrepancy-service çuval kimliğini bilmez, yalnızca teslimde okutulan anahtarı bilir. Anahtar tek kullanımlık
-- olduğu için itiraz içinde çuvalı tek başına tanımlar.
ALTER TABLE quality_claim_bag DROP CONSTRAINT quality_claim_bag_pkey;
ALTER TABLE quality_claim_bag ALTER COLUMN bag_id DROP NOT NULL;
ALTER TABLE quality_claim_bag ADD PRIMARY KEY (claim_id, unique_key);

CREATE INDEX quality_claim_bag_key_idx ON quality_claim_bag (unique_key);

-- Numune / lab talebinin notu (ör. "fasoncudaki numune ile şahit numuneyi karşılaştır").
ALTER TABLE inspection ADD COLUMN request_note text;

CREATE INDEX inspection_discrepancy_idx ON inspection (discrepancy_id);
