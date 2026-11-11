-- Ambalaj tipi yalnızca çuvaldır (BAG); koli, palet ve bobin kaldırıldı.
UPDATE shipment_bag SET package_type = 'BAG' WHERE package_type <> 'BAG';
ALTER TABLE shipment_bag DROP CONSTRAINT IF EXISTS shipment_bag_package_type_check;
ALTER TABLE shipment_bag ADD CONSTRAINT shipment_bag_package_type_check CHECK (package_type = 'BAG');

DELETE FROM md_tare_standard WHERE packaging_type <> 'BAG';
ALTER TABLE md_tare_standard DROP CONSTRAINT IF EXISTS md_tare_standard_packaging_type_check;
ALTER TABLE md_tare_standard ADD CONSTRAINT md_tare_standard_packaging_type_check CHECK (packaging_type = 'BAG');
