-- Fiş okuma kaldırıldı: fiş yalnızca fotoğraf olarak saklanır. Okuma ve "doğrulanamadı" kayıtları artık yazılmaz.
DROP TABLE IF EXISTS ticket_unverified;
DROP TABLE IF EXISTS ticket_reading;
