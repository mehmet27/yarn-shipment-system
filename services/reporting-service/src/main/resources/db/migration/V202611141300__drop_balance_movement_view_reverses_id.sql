-- Kaynak defterdeki kolonla birlikte okuma modelinden de kaldırılır; hiçbir sorgu kullanmıyor.
ALTER TABLE balance_movement_view DROP COLUMN IF EXISTS reverses_id;
