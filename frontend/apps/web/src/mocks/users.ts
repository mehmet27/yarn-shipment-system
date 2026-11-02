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
    'DISPATCH_VIEW',
    'DRIVER_DECLARATION_VIEW',
    'DELIVERY_VIEW',
    'EVIDENCE_VIEW',
    'EVIDENCE_INVALIDATE',
    'CORRECTION_CREATE',
    'QUALITY_CLAIM_OPEN',
    'DISCREPANCY_VIEW',
    'DISCREPANCY_RESOLVE',
    'AUDIT_VIEW',
    'REPORT_VIEW',
  ],
  WAREHOUSE: [
    'MASTER_DATA_VIEW',
    'BAG_CREATE',
    'SHIPMENT_VIEW',
    'SHIPMENT_CREATE',
    'DISPATCH_RECORD',
    'DISPATCH_VIEW',
    'DRIVER_DECLARATION_VIEW',
    'DELIVERY_VIEW',
    'EVIDENCE_UPLOAD_DISPATCH',
    'EVIDENCE_VIEW',
  ],
  DRIVER: [
    'SHIPMENT_VIEW',
    'DISPATCH_VIEW',
    'DRIVER_DECLARATION_RECORD',
    'DRIVER_DECLARATION_VIEW',
    'EVIDENCE_UPLOAD_DRIVER',
    'EVIDENCE_VIEW',
  ],
  SUBCONTRACTOR: [
    'SHIPMENT_VIEW',
    'DISPATCH_VIEW',
    'DELIVERY_RECORD',
    'DELIVERY_VIEW',
    'EVIDENCE_UPLOAD_DELIVERY',
    'EVIDENCE_VIEW',
    'QUALITY_CLAIM_OPEN',
    'DISCREPANCY_VIEW',
  ],
  QUALITY: [
    'MASTER_DATA_VIEW',
    'LOT_QUALITY_LOCK',
    'SHIPMENT_VIEW',
    'DISPATCH_VIEW',
    'DRIVER_DECLARATION_VIEW',
    'DELIVERY_VIEW',
    'EVIDENCE_UPLOAD_CLAIM',
    'EVIDENCE_VIEW',
    'QUALITY_CLAIM_OPEN',
    'DISCREPANCY_VIEW',
    'DISCREPANCY_RESOLVE',
    'AUDIT_VIEW',
    'REPORT_VIEW',
  ],
  MANAGER: [
    'MASTER_DATA_VIEW',
    'SHIPMENT_VIEW',
    'DISPATCH_VIEW',
    'DRIVER_DECLARATION_VIEW',
    'DELIVERY_VIEW',
    'EVIDENCE_VIEW',
    'DISCREPANCY_VIEW',
    'AUDIT_VIEW',
    'REPORT_VIEW',
  ],
};

const FULL_NAMES: Readonly<Record<Role, string>> = {
  ADMIN: 'Ayşe Yönetici',
  WAREHOUSE: 'Deniz Depo',
  DRIVER: 'Şahin Şoför',
  SUBCONTRACTOR: 'Fatma Fasoncu',
  QUALITY: 'Kemal Kalite',
  MANAGER: 'Yusuf Müdür',
};

const SCOPE_BY_ROLE: Readonly<
  Partial<Record<Role, Pick<CurrentUser, 'driverId' | 'subcontractorId'>>>
> = {
  DRIVER: { driverId: 'drv-0' },
  SUBCONTRACTOR: { subcontractorId: 'sub-0' },
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
    ...SCOPE_BY_ROLE[role],
  };
}
