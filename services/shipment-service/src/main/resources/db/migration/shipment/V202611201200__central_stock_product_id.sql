-- Merkez iplik stoku iplik tanımına (md_product) bağlanır: stok adla değil product_id ile gruplanır, yeniden
-- adlandırma stoğu bölmez. yarn_type hareket anındaki adın kopyası olarak kalır (eski satırlar ve gösterim için).
ALTER TABLE central_stock_movement ADD COLUMN product_id uuid REFERENCES md_product (id);

COMMENT ON COLUMN central_stock_movement.product_id IS
    'İplik tanımı; yalnızca tanımla eşlenemeyen eski serbest metin satırlarında boştur';

-- Eski satırlar: ad (büyük/küçük harf ve baştaki/sondaki boşluk yok sayılır) tek bir tanımla eşleşiyorsa bağlanır.
-- Defter yalnız eklemelidir; geri doldurma için tetikleyici yalnızca bu migration içinde kapatılır.
ALTER TABLE central_stock_movement DISABLE TRIGGER central_stock_movement_append_only;

UPDATE central_stock_movement m
SET product_id = x.product_id
FROM (
    SELECT lower(btrim(name)) AS key, min(id::text)::uuid AS product_id
    FROM md_product
    GROUP BY lower(btrim(name))
    HAVING count(*) = 1
) x
WHERE m.product_id IS NULL AND lower(btrim(m.yarn_type)) = x.key;

UPDATE central_stock_movement m
SET product_id = p.id
FROM md_product p
WHERE m.product_id IS NULL AND p.id::text = m.yarn_type;

ALTER TABLE central_stock_movement ENABLE TRIGGER central_stock_movement_append_only;

CREATE INDEX central_stock_movement_product_idx ON central_stock_movement (product_id);
