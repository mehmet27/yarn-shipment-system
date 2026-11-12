-- Yalnızca yerel geliştirme (local profili): örnek sevkiyatlar (shipment şeması, R__local_sample_shipments) için
-- izleme kaydı. Eskiden bu kayıtlar sevkiyat olaylarının Kafka'dan yeniden okunmasıyla oluşurdu; olay geçmişi
-- tutulmadığı için örnek veri doğrudan shipment.shipment'tan türetilir. Gerçek ortamda kayıtlar sevkiyat olaylarıyla
-- (süreç içi) yazılır; bu dosya orada çalışmaz. Tekrarlanabilir migration; kayıtlar ON CONFLICT ile tekrarlanmaz.
INSERT INTO shipment_watch (shipment_id, shipment_no, site_id, subcontractor_id, shipment_created_at,
                            dispatched_at, dispatch_net_kg, completed_at, closed_at, created_at, created_by)
SELECT s.id, s.shipment_no, s.site_id, s.subcontractor_id, s.created_at,
       CASE WHEN s.status IN ('AWAITING_DELIVERY', 'COMPLETED') THEN s.created_at + interval '2 hours' END,
       CASE WHEN s.status IN ('AWAITING_DELIVERY', 'COMPLETED') THEN s.planned_net_kg END,
       CASE WHEN s.status = 'COMPLETED' THEN s.created_at + interval '2 days' END,
       CASE WHEN s.status = 'COMPLETED' THEN s.created_at + interval '2 days' END,
       s.created_at, '0192f0a0-0000-7000-8000-00000000d001'
FROM shipment.shipment s
WHERE s.id::text LIKE '0192f0a0-0000-7000-8000-0000000a%'
ON CONFLICT (shipment_id) DO NOTHING;
