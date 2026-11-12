-- Ters hareket kolonu hiçbir yerde yazılmadı ve okunmadı; düzeltme CORRECTION türüyle yeni satır olarak yazılır.
ALTER TABLE balance_movement DROP COLUMN IF EXISTS reverses_id;
