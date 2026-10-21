import { httpClient } from './httpClient';

/**
 * Yetkilendirme analizi, rol-izin matrisi (libs/common-security RolePermissionMatrix). Backend OpenAPI sözleşmesi yayınlanınca
 * bu dosya Orval çıktısıyla değiştirilir (FE-API-01).
 */
export type Permission =
  | 'MASTER_DATA_VIEW'
  | 'MASTER_DATA_MANAGE'
  | 'USER_MANAGE'
  | 'SETTINGS_MANAGE'
  | 'LOT_QUALITY_LOCK'
  | 'SHIPMENT_VIEW'
  | 'SHIPMENT_CREATE'
  | 'SHIPMENT_CANCEL'
  | 'DISPATCH_RECORD'
  | 'DISPATCH_VIEW'
  | 'DRIVER_DECLARATION_RECORD'
  | 'DRIVER_DECLARATION_VIEW'
  | 'DELIVERY_RECORD'
  | 'DELIVERY_VIEW'
  | 'EVIDENCE_UPLOAD_DISPATCH'
  | 'EVIDENCE_UPLOAD_DRIVER'
  | 'EVIDENCE_UPLOAD_DELIVERY'
  | 'EVIDENCE_UPLOAD_CLAIM'
  | 'EVIDENCE_VIEW'
  | 'EVIDENCE_INVALIDATE'
  | 'CORRECTION_CREATE'
  | 'QUALITY_CLAIM_OPEN'
  | 'DISCREPANCY_VIEW'
  | 'DISCREPANCY_RESOLVE'
  | 'AUDIT_VIEW'
  | 'REPORT_VIEW';

export type Role = 'ADMIN' | 'WAREHOUSE' | 'DRIVER' | 'SUBCONTRACTOR' | 'QUALITY' | 'MANAGER';

export type CurrentUser = {
  userId: string;
  fullName: string;
  roles: readonly Role[];
  permissions: readonly Permission[];
  siteIds: readonly string[];
  subcontractorId?: string;
  driverId?: string;
  mfaVerified: boolean;
};

export const ME_PATH = '/api/v1/me';
export const LOGIN_PATH = '/oauth2/authorization/keycloak';

export async function getCurrentUser(): Promise<CurrentUser> {
  const response = await httpClient.get<CurrentUser>(ME_PATH);
  return response.data;
}

/** Çıkış gateway'de; CSRF başlığıyla POST (yetkilendirme analizi). */
export async function logout(): Promise<void> {
  await httpClient.post('/logout');
}
