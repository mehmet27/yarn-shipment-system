-- Sevkiyata atanan ambalajın türü (çuval, koli, palet, bobin). Çıkış tartımındaki standart dara türe göre seçilir.
-- Eski kayıtların hepsi çuvaldı.

ALTER TABLE shipment_bag
    ADD COLUMN package_type text NOT NULL DEFAULT 'BAG' CHECK (package_type IN ('BAG', 'BOX', 'PALLET', 'BOBBIN'));
