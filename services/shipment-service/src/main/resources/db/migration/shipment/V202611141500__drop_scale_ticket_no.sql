-- Elle yazılan kantar fişi numarası kaldırıldı; fiş yalnızca fotoğraf olarak saklanır.
ALTER TABLE weighing DROP COLUMN IF EXISTS scale_ticket_no;
