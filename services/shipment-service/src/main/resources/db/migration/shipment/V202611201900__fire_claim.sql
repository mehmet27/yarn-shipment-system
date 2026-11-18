-- Haklı kalite itirazı kilosu bakiyeden hemen düşmez: fire dosyasında "faturalanacak" kiloya eklenir ve müdür fatura
-- kesince fasoncunun iplik bakiyesinden düşer (INVOICE hareketi). Karar bu tabloya yazılır; fire hesabı (I) bu kaydı
-- ve eski CLAIM_ADJUSTMENT hareketlerini birlikte sayar. Bu sürümden önce karara bağlanmış itirazlar zaten bakiyeden
-- düşülmüştür ve yeniden faturalanmaz.
CREATE TABLE fire_claim (
    discrepancy_id uuid           PRIMARY KEY,
    shipment_id    uuid           NOT NULL REFERENCES shipment (id),
    claimed_kg     numeric(12, 3) NOT NULL CHECK (claimed_kg > 0),
    decided_by     uuid           NOT NULL,
    decided_at     timestamptz    NOT NULL,
    created_at     timestamptz    NOT NULL
);

CREATE INDEX fire_claim_shipment_idx ON fire_claim (shipment_id);

CREATE TRIGGER fire_claim_append_only
    BEFORE UPDATE OR DELETE ON fire_claim
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

-- Faturanın haklı itiraz kilosunu karşılayan kısmı: kalan itiraz kilosu önce karşılanır, fazlası eksik kumaşa gider.
ALTER TABLE fire_invoice
    ADD COLUMN claim_kg numeric(12, 3) NOT NULL DEFAULT 0 CHECK (claim_kg >= 0),
    ADD CONSTRAINT fire_invoice_claim_within_kg CHECK (claim_kg <= kg);

GRANT SELECT, INSERT ON fire_claim TO ${app_role};
