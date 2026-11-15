-- Rol izinleri artık veritabanından okunur ve yönetici tarafından Yetkiler ekranından düzenlenir. Yönetici (ADMIN) ve
-- sistem rolü kodda sabittir, bu tabloda yer almaz. Başlangıç içeriği koddaki varsayılan matristir.
-- Yalnızca yöneticide olan izinler (USER_MANAGE, SETTINGS_MANAGE, PERMISSION_MANAGE) başka role yazılamaz.

CREATE TABLE role_permission (
    role       text        NOT NULL CHECK (role IN ('WAREHOUSE', 'QUALITY', 'MANAGER', 'GENERAL_MANAGER')),
    permission text        NOT NULL CHECK (permission NOT IN ('USER_MANAGE', 'SETTINGS_MANAGE', 'PERMISSION_MANAGE')),
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    PRIMARY KEY (role, permission)
);

COMMENT ON TABLE role_permission IS 'Rol başına verilen izinler; Yetkiler ekranından düzenlenir, değişiklik audit_local trigger''ıyla izlenir';

INSERT INTO role_permission (role, permission) VALUES
    ('WAREHOUSE', 'MASTER_DATA_VIEW'),
    ('WAREHOUSE', 'SHIPMENT_VIEW'),
    ('WAREHOUSE', 'DISPATCH_RECORD'),
    ('WAREHOUSE', 'DISPATCH_VIEW'),
    ('WAREHOUSE', 'EVIDENCE_UPLOAD_DISPATCH'),
    ('WAREHOUSE', 'EVIDENCE_VIEW'),
    ('WAREHOUSE', 'YARN_RETURN_RECORD'),
    ('WAREHOUSE', 'CENTRAL_STOCK_VIEW'),
    ('WAREHOUSE', 'CENTRAL_STOCK_RECORD'),
    ('QUALITY', 'MASTER_DATA_VIEW'),
    ('QUALITY', 'LOT_QUALITY_LOCK'),
    ('QUALITY', 'SHIPMENT_VIEW'),
    ('QUALITY', 'DISPATCH_VIEW'),
    ('QUALITY', 'EVIDENCE_UPLOAD_CLAIM'),
    ('QUALITY', 'EVIDENCE_VIEW'),
    ('QUALITY', 'QUALITY_CLAIM_OPEN'),
    ('QUALITY', 'DISCREPANCY_VIEW'),
    ('QUALITY', 'DISCREPANCY_RESOLVE'),
    ('QUALITY', 'AUDIT_VIEW'),
    ('QUALITY', 'REPORT_VIEW'),
    ('QUALITY', 'FABRIC_RECEIPT_RECORD'),
    ('QUALITY', 'BALANCE_VIEW'),
    ('QUALITY', 'CENTRAL_STOCK_VIEW'),
    ('QUALITY', 'CENTRAL_PRODUCTION_RECORD'),
    ('MANAGER', 'MASTER_DATA_VIEW'),
    ('MANAGER', 'MASTER_DATA_MANAGE'),
    ('MANAGER', 'LOT_QUALITY_LOCK'),
    ('MANAGER', 'SHIPMENT_VIEW'),
    ('MANAGER', 'SHIPMENT_CREATE'),
    ('MANAGER', 'SHIPMENT_CANCEL'),
    ('MANAGER', 'SHIPMENT_COMPLETE'),
    ('MANAGER', 'DISPATCH_VIEW'),
    ('MANAGER', 'EVIDENCE_UPLOAD_CLAIM'),
    ('MANAGER', 'EVIDENCE_VIEW'),
    ('MANAGER', 'EVIDENCE_INVALIDATE'),
    ('MANAGER', 'CORRECTION_CREATE'),
    ('MANAGER', 'QUALITY_CLAIM_OPEN'),
    ('MANAGER', 'DISCREPANCY_VIEW'),
    ('MANAGER', 'AUDIT_VIEW'),
    ('MANAGER', 'REPORT_VIEW'),
    ('MANAGER', 'YARN_RETURN_RECORD'),
    ('MANAGER', 'FIRE_RATE_EDIT'),
    ('MANAGER', 'INVOICE_MARK'),
    ('MANAGER', 'BALANCE_VIEW'),
    ('MANAGER', 'WEIGHT_MISMATCH_RESOLVE'),
    ('MANAGER', 'CENTRAL_STOCK_VIEW'),
    ('GENERAL_MANAGER', 'REPORT_VIEW'),
    ('GENERAL_MANAGER', 'BALANCE_VIEW'),
    ('GENERAL_MANAGER', 'MASTER_DATA_VIEW');

CREATE TRIGGER role_permission_audit_local
    AFTER INSERT OR UPDATE OR DELETE ON role_permission
    FOR EACH ROW EXECUTE FUNCTION audit_local_capture();

GRANT SELECT, INSERT, UPDATE, DELETE ON role_permission TO ${app_role};
