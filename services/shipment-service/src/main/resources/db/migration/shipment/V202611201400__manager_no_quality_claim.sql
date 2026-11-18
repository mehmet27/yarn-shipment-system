-- Kalite itirazını yalnızca kalite açar. Müdür itiraz açma ve itiraz belgesi yükleme izinlerini artık varsayılan
-- olarak almaz; mevcut kurulumlarda müdürden kaldırılır. Müdür itirazları ve uyuşmazlıkları görmeye devam eder.
DELETE FROM role_permission
WHERE role = 'MANAGER' AND permission IN ('QUALITY_CLAIM_OPEN', 'EVIDENCE_UPLOAD_CLAIM');
