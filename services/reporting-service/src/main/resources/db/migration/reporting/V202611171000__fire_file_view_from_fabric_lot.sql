-- Kumaş girişi fabric_lot tablosuna birleşti (shipment V202611161120). fire_file_view K (gelen kumaş) toplamını artık
-- doğrudan fabric_lot'tan okur; yalnız fasoncudan gelen kayıtlar sevkiyata bağlıdır (merkez üretimin shipment_id'si boştur).
-- Sütunlar aynı kaldığı için görünüm yerinde değiştirilir.
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
       coalesce(s.locked_at, f.created_at) AS completed_at
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
) fr ON true;

-- Geçici uyumluluk görünümü artık okunmuyor.
DROP VIEW shipment.fabric_receipt;
