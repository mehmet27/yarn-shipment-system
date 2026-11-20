-- Uyuşmazlık geçmişi satırı var olan bir uyuşmazlığa bağlı olmalıdır. İkisi aynı şemadadır; inspection ve
-- quality_claim bu bağı zaten yabancı anahtarla kurar, discrepancy_activity'de eksikti. Uyuşmazlık silinemediği
-- (discrepancy_no_delete) ve geçmiş yalnızca var olan uyuşmazlığa yazıldığı için mevcut veride sahipsiz satır olamaz.
ALTER TABLE discrepancy_activity
    ADD CONSTRAINT discrepancy_activity_discrepancy_id_fkey FOREIGN KEY (discrepancy_id) REFERENCES discrepancy (id);
