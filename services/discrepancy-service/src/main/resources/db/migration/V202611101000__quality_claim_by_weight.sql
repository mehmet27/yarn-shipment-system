-- Kalite itirazı kilogram bazlıdır (analiz v2.0, 5.12 ve A5): itiraz çuval değil, kilo ile ve fabrika personelince
-- açılır; kumaş girişi (fabric receipt) ileride bağlanacağı için kimliği şimdiden isteğe bağlı tutulur.
--
-- İtiraz hedefi artık comparison_input yerine shipment_watch'tan okunur (tamamlanan sevkiyat). Bunun için izleme
-- kaydına tamamlanma zamanı ve çıkış net kilosu eklenir; closed_at tamamlanma ile iptali ayırt etmez.
ALTER TABLE shipment_watch
    ADD COLUMN completed_at     timestamptz,
    ADD COLUMN dispatch_net_kg  numeric(14, 3) CHECK (dispatch_net_kg IS NULL OR dispatch_net_kg >= 0);

COMMENT ON COLUMN shipment_watch.completed_at IS 'SevkiyatTamamlandi zamanı; kalite itiraz süresi (3 iş günü) buradan sayılır';
COMMENT ON COLUMN shipment_watch.dispatch_net_kg IS 'SevkiyatCikisYapti net kilosu; itiraz edilen kilo bunu aşamaz';

-- Önceden karşılaştırılmış (teslim edilmiş) sevkiyatlar için best-effort doldurma.
UPDATE shipment_watch w
   SET completed_at = w.closed_at, dispatch_net_kg = c.dispatch_net_kg
  FROM comparison_input c
 WHERE c.shipment_id = w.shipment_id AND w.closed_at IS NOT NULL AND w.completed_at IS NULL;

-- Eski çuval bazlı itirazlar geçmiş olarak kalır (quality_claim_bag silinmez); onlarda kilo boştur.
ALTER TABLE quality_claim
    ADD COLUMN claimed_kg        numeric(14, 3) CHECK (claimed_kg IS NULL OR claimed_kg > 0),
    ADD COLUMN fabric_receipt_id uuid,
    ALTER COLUMN subcontractor_id DROP NOT NULL;

COMMENT ON COLUMN quality_claim.claimed_kg IS 'İtiraz edilen kilo; çuval bazlı eski itirazlarda boş';
COMMENT ON COLUMN quality_claim.fabric_receipt_id IS 'İsteğe bağlı kumaş girişi kimliği (kumaş girişi henüz yok)';
