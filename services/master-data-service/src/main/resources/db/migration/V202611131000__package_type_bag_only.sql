-- Ambalaj tipi yalnızca çuvaldır (BAG); koli, palet ve bobin kaldırıldı.
UPDATE product SET packaging_type = 'BAG' WHERE packaging_type <> 'BAG';
ALTER TABLE product DROP CONSTRAINT IF EXISTS product_packaging_type_check;
ALTER TABLE product ADD CONSTRAINT product_packaging_type_check CHECK (packaging_type = 'BAG');

DELETE FROM tare_standard WHERE packaging_type <> 'BAG';
ALTER TABLE tare_standard DROP CONSTRAINT IF EXISTS tare_standard_packaging_type_check;
ALTER TABLE tare_standard ADD CONSTRAINT tare_standard_packaging_type_check CHECK (packaging_type = 'BAG');
