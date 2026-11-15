-- İplik dönüşünü yalnızca depo kaydeder (fasoncudan depoya). Müdür bu izni artık varsayılan olarak almaz; mevcut
-- kurulumlarda müdürden kaldırılır. İzin Yetkiler ekranından yeniden verilebilir.
-- Eski "fasoncudan gelen iplik" (yarn_return.direction = 'FROM_SUBCONTRACTOR') kayıtları eklemeli tabloda olduğu için
-- silinmez; geçmiş olarak okunmaya devam eder.
DELETE FROM role_permission WHERE role = 'MANAGER' AND permission = 'YARN_RETURN_RECORD';
