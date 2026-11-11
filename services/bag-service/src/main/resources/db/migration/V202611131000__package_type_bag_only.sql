-- Ambalaj tipi yalnızca çuvaldır (BAG); koli, palet ve bobin kaldırıldı.
UPDATE bag SET package_type = 'BAG' WHERE package_type <> 'BAG';
ALTER TABLE bag DROP CONSTRAINT IF EXISTS bag_package_type_check;
ALTER TABLE bag ADD CONSTRAINT bag_package_type_check CHECK (package_type = 'BAG');
