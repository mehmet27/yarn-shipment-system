-- Haklı kalite itirazı kilosu bakiyeden hemen düşmez, faturalanacak kiloya eklenir (shipment.fire_claim):
--  * claim_kg: fire hesabındaki I; eski itirazlar (CLAIM_ADJUSTMENT) ile faturayla düşülecek itirazlar birlikte.
--  * invoiced_kg: yalnız eksik kumaşı karşılayan fatura kilosu (fire yüzdesi ve kalan eksik buna göre hesaplanır).
--  * claim_to_invoice_kg: itirazdan henüz faturalanmamış kilo.
CREATE OR REPLACE VIEW fire_file_view AS
SELECT f.shipment_id,
       s.shipment_no,
       f.subcontractor_id,
       coalesce(sub.title, s.subcontractor_title, '') AS subcontractor_name,
       s.site_id,
       f.sent_kg,
       coalesce(r.returned_kg, 0)::numeric(14, 3) AS returned_yarn_kg,
       coalesce(fr.fabric_kg, 0)::numeric(14, 3) AS fabric_kg,
       f.fire_rate,
       CASE WHEN f.status = 'INVOICED' THEN 'CLOSED' ELSE f.status END AS status,
       f.invoice_no,
       (f.invoiced_on::timestamp AT TIME ZONE 'Europe/Istanbul') AS invoiced_at,
       coalesce(s.locked_at, f.created_at) AS completed_at,
       (coalesce(cl.claim_kg, 0) + coalesce(pc.pending_claim_kg, 0))::numeric(14, 3) AS claim_kg,
       (coalesce(iv.invoiced_kg, 0) - coalesce(ci.claim_invoiced_kg, 0))::numeric(14, 3) AS invoiced_kg,
       greatest(0, coalesce(pc.pending_claim_kg, 0) - coalesce(ci.claim_invoiced_kg, 0))::numeric(14, 3)
           AS claim_to_invoice_kg
FROM shipment.fire_settlement f
JOIN shipment.shipment s ON s.id = f.shipment_id
LEFT JOIN shipment.md_subcontractor sub ON sub.id = f.subcontractor_id
LEFT JOIN LATERAL (
    SELECT sum(y.net_kg) AS returned_kg
    FROM shipment.yarn_return y
    WHERE y.shipment_id = f.shipment_id AND y.direction = 'RETURN'
) r ON true
LEFT JOIN LATERAL (
    SELECT sum(c.net_kg) AS fabric_kg
    FROM shipment.fabric_lot c
    WHERE c.shipment_id = f.shipment_id AND c.source = 'SUBCONTRACTOR'
) fr ON true
LEFT JOIN LATERAL (
    SELECT greatest(0, -sum(m.net_kg)) AS claim_kg
    FROM shipment.balance_movement m
    WHERE m.shipment_id = f.shipment_id AND m.kind = 'CLAIM_ADJUSTMENT'
) cl ON true
LEFT JOIN LATERAL (
    SELECT sum(c.claimed_kg) AS pending_claim_kg
    FROM shipment.fire_claim c
    WHERE c.shipment_id = f.shipment_id
) pc ON true
LEFT JOIN LATERAL (
    SELECT greatest(0, -sum(m.net_kg)) AS invoiced_kg
    FROM shipment.balance_movement m
    WHERE m.shipment_id = f.shipment_id AND m.kind = 'INVOICE'
) iv ON true
LEFT JOIN LATERAL (
    SELECT sum(i.claim_kg) AS claim_invoiced_kg
    FROM shipment.fire_invoice i
    WHERE i.shipment_id = f.shipment_id
) ci ON true;
