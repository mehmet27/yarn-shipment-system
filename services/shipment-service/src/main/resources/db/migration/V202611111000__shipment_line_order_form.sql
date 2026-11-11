-- Akış dokümanı: sipariş formu reçetenin yerine geçer. Excel'deki başlıklar reçete satırına eklenir.

ALTER TABLE shipment_line
    ADD COLUMN fabric_type  text CHECK (fabric_type IS NULL OR length(btrim(fabric_type)) > 0),
    ADD COLUMN lycra        text CHECK (lycra IS NULL OR length(btrim(lycra)) > 0),
    ADD COLUMN width_type   text CHECK (width_type IS NULL OR width_type IN ('TUBE', 'MAY', 'OPEN_WIDTH')),
    ADD COLUMN yarn_length  text CHECK (yarn_length IS NULL OR length(btrim(yarn_length)) > 0),
    ADD COLUMN puss_fine    text CHECK (puss_fine IS NULL OR length(btrim(puss_fine)) > 0),
    ADD COLUMN unit_price   numeric(12, 4) CHECK (unit_price IS NULL OR unit_price >= 0);

COMMENT ON COLUMN shipment_line.fabric_type IS 'Sipariş formu: kumaş cinsi';
COMMENT ON COLUMN shipment_line.lycra IS 'Sipariş formu: LYC (ör. 30 denye)';
COMMENT ON COLUMN shipment_line.width_type IS 'Sipariş formu: tüp / may / açık en';
COMMENT ON COLUMN shipment_line.yarn_length IS 'Sipariş formu: iplik uzunluğu / lycra';
COMMENT ON COLUMN shipment_line.puss_fine IS 'Sipariş formu: puss-fine';
COMMENT ON COLUMN shipment_line.unit_price IS 'Sipariş formu: birim fiyat, TL/kg';
