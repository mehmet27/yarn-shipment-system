-- Haklı kalite itirazı için süre sınırı yoktur; dosya kapanıp faturalandıktan sonra gelen haklı itiraz da faturalanmalı.
-- Kapanmış (INVOICED, CLOSED) dosya yalnız faturalanacağa dönebilir (TO_INVOICE); başka hiçbir alan değişmez.
-- Önceki faturalar fire_invoice kaydında kalır; dosyadaki son fatura alanları yeni fatura girilene kadar boşalır.
CREATE OR REPLACE FUNCTION guard_fire_settlement_update()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.status IN ('INVOICED', 'CLOSED') AND NEW.status <> 'TO_INVOICE' THEN
        RAISE EXCEPTION 'Fire settlement is closed shipmentId=%', OLD.shipment_id USING ERRCODE = 'YS003';
    END IF;
    IF NEW.shipment_id <> OLD.shipment_id OR NEW.subcontractor_id <> OLD.subcontractor_id
            OR NEW.sent_kg <> OLD.sent_kg THEN
        RAISE EXCEPTION 'Immutable column changed table=fire_settlement id=%', OLD.shipment_id USING ERRCODE = 'YS002';
    END IF;
    RETURN NEW;
END;
$$;
