import type { Permission } from '@iplik/api-client';

export type PermissionGroup = {
  readonly key: string;
  readonly permissions: readonly Permission[];
};

/** Yetkiler ekranında izinlerin ekrana göre gruplanışı; her izin tam bir grupta yer alır. */
export const PERMISSION_GROUPS: readonly PermissionGroup[] = [
  {
    key: 'masterData',
    permissions: ['MASTER_DATA_VIEW', 'MASTER_DATA_MANAGE', 'LOT_QUALITY_LOCK'],
  },
  {
    key: 'shipment',
    permissions: [
      'SHIPMENT_VIEW',
      'SHIPMENT_CREATE',
      'SHIPMENT_CANCEL',
      'SHIPMENT_COMPLETE',
      'DISPATCH_VIEW',
      'DISPATCH_RECORD',
      'WEIGHT_MISMATCH_RESOLVE',
      'FIRE_RATE_EDIT',
      'INVOICE_MARK',
    ],
  },
  {
    key: 'evidence',
    permissions: [
      'EVIDENCE_VIEW',
      'EVIDENCE_UPLOAD_DISPATCH',
      'EVIDENCE_UPLOAD_CLAIM',
      'EVIDENCE_INVALIDATE',
      'CORRECTION_CREATE',
    ],
  },
  {
    key: 'quality',
    permissions: [
      'DISCREPANCY_VIEW',
      'DISCREPANCY_RESOLVE',
      'QUALITY_CLAIM_OPEN',
      'FABRIC_RECEIPT_RECORD',
    ],
  },
  {
    key: 'stock',
    permissions: [
      'BALANCE_VIEW',
      'YARN_RETURN_RECORD',
      'CENTRAL_STOCK_VIEW',
      'CENTRAL_STOCK_RECORD',
      'CENTRAL_PRODUCTION_RECORD',
    ],
  },
  { key: 'reporting', permissions: ['REPORT_VIEW', 'AUDIT_VIEW'] },
  { key: 'system', permissions: ['USER_MANAGE', 'SETTINGS_MANAGE', 'PERMISSION_MANAGE'] },
];
