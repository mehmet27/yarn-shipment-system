-- Müdür kalite itirazını "haklı, faturalandı" ya da "haksız" diye karara bağlar. Mevcut kurulumlarda müdüre eklenir;
-- kalitenin kararı ve laboratuvar kontrolü yetkisi değişmez.
INSERT INTO role_permission (role, permission) VALUES ('MANAGER', 'DISCREPANCY_DECIDE')
ON CONFLICT DO NOTHING;
