-- Fire raporu, sevkiyat ekranındaki fire dosyasıyla aynı sayıları göstersin: haklı kalite itirazı (I) fire tabanından ve
-- gelen kumaştan düşülür (FireCalculation: T = G − D − I, K − I). Görünüme claim_kg sütunu sona eklenir; diğer sütunlar aynı.
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
       coalesce(cl.claim_kg, 0)::numeric(14, 3) AS claim_kg
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
) cl ON true;
