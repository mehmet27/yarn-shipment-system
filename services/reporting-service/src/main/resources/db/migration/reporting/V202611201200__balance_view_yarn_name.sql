-- Bakiye defterinde iplik, reçetede listeden seçilince UUID metni olarak saklanır; rapor ve matris ad göstersin.
-- Tanım bulunamazsa (serbest metin / eski kayıt) saklanan metin aynen kalır.
CREATE OR REPLACE VIEW balance_movement_view AS
SELECT m.id,
       m.subcontractor_id,
       coalesce(sub.title, s.subcontractor_title, '') AS subcontractor_name,
       coalesce(p.name, m.yarn_type) AS yarn_type,
       m.shipment_id,
       s.site_id,
       m.kind,
       m.net_kg,
       m.occurred_at
FROM shipment.balance_movement m
LEFT JOIN shipment.shipment s ON s.id = m.shipment_id
LEFT JOIN shipment.md_subcontractor sub ON sub.id = m.subcontractor_id
LEFT JOIN shipment.md_product p ON p.id::text = m.yarn_type;
