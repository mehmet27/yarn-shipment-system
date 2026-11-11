import type { CurrentUser, Permission, Role } from '@iplik/api-client';

/** Yetkilendirme analizi rol-izin matrisinin sahte API karşılığı ("kendi" kısıtı backend'dedir). */
const PERMISSIONS_BY_ROLE: Readonly<Record<Role, readonly Permission[]>> = {
  ADMIN: [
    'MASTER_DATA_VIEW',
    'MASTER_DATA_MANAGE',
    'USER_MANAGE',
    'SETTINGS_MANAGE',
    'LOT_QUALITY_LOCK',
    'BAG_CREATE',
    'SHIPMENT_VIEW',
    'SHIPMENT_CREATE',
    'SHIPMENT_CANCEL',
    'SHIPMENT_COMPLETE',
    'DISPATCH_VIEW',
    'EVIDENCE_UPLOAD_CLAIM',
    'EVIDENCE_VIEW',
    'EVIDENCE_INVALIDATE',
    'CORRECTION_CREATE',
    'QUALITY_CLAIM_OPEN',
    'DISCREPANCY_VIEW',
    'DISCREPANCY_RESOLVE',
    'AUDIT_VIEW',
    'REPORT_VIEW',
    'BALANCE_VIEW',
  ],
  WAREHOUSE: [
    'MASTER_DATA_VIEW',
    'BAG_CREATE',
    'SHIPMENT_VIEW',
    'DISPATCH_RECORD',
    'DISPATCH_VIEW',
    'EVIDENCE_UPLOAD_DISPATCH',
    'EVIDENCE_VIEW',
    'YARN_RETURN_RECORD',
  ],
  QUALITY: [
    'MASTER_DATA_VIEW',
    'LOT_QUALITY_LOCK',
    'SHIPMENT_VIEW',
    'DISPATCH_VIEW',
    'EVIDENCE_UPLOAD_CLAIM',
    'EVIDENCE_VIEW',
    'QUALITY_CLAIM_OPEN',
    'DISCREPANCY_VIEW',
    'DISCREPANCY_RESOLVE',
    'AUDIT_VIEW',
    'REPORT_VIEW',
    'FABRIC_RECEIPT_RECORD',
    'BALANCE_VIEW',
  ],
  MANAGER: [
    'MASTER_DATA_VIEW',
    'SHIPMENT_VIEW',
    'SHIPMENT_CREATE',
    'SHIPMENT_CANCEL',
    'SHIPMENT_COMPLETE',
    'DISPATCH_VIEW',
    'EVIDENCE_UPLOAD_CLAIM',
    'EVIDENCE_VIEW',
    'QUALITY_CLAIM_OPEN',
    'DISCREPANCY_VIEW',
    'AUDIT_VIEW',
    'REPORT_VIEW',
    'FABRIC_RECEIPT_RECORD',
    'YARN_RETURN_RECORD',
    'FIRE_RATE_EDIT',
    'INVOICE_MARK',
    'BALANCE_VIEW',
    'WEIGHT_MISMATCH_RESOLVE',
  ],
  GENERAL_MANAGER: ['REPORT_VIEW', 'BALANCE_VIEW'],
};

const FULL_NAMES: Readonly<Record<Role, string>> = {
  ADMIN: 'Ayşe Yönetici',
  WAREHOUSE: 'Deniz Depo',
  QUALITY: 'Kemal Kalite',
  MANAGER: 'Yusuf Müdür',
  GENERAL_MANAGER: 'Gül Genel Müdür',
};

export const MOCK_ROLES = Object.keys(PERMISSIONS_BY_ROLE).filter(
  (key): key is Role => key in PERMISSIONS_BY_ROLE,
);

export function createMockUser(role: Role): CurrentUser {
  return {
    userId: `user-${role.toLowerCase()}`,
    fullName: FULL_NAMES[role],
    roles: [role],
    permissions: [...PERMISSIONS_BY_ROLE[role]],
    siteIds: ['site-merkez'],
    mfaVerified: true,
  };
}
