-- Tutar (TL) bilgisini yalnızca müdür ve genel müdür görür. Genel müdür merkez depodaki ipliği de görür.
INSERT INTO role_permission (role, permission) VALUES
    ('MANAGER', 'AMOUNT_VIEW'),
    ('GENERAL_MANAGER', 'AMOUNT_VIEW'),
    ('GENERAL_MANAGER', 'CENTRAL_STOCK_VIEW')
ON CONFLICT DO NOTHING;
