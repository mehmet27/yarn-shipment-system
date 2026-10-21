import { httpClient } from './httpClient';

/**
 * Yetkilendirme analizi, rol-izin matrisi. Backend OpenAPI sözleşmesi yayınlanınca
 * bu dosya Orval çıktısıyla değiştirilir (FE-API-01).
 */
export type Permission =
  | 'TANIM_GORUNTULE'
  | 'TANIM_YONET'
  | 'KULLANICI_YONET'
  | 'AYAR_YONET'
  | 'PARTI_KALITE_BELIRLE'
  | 'SEVKIYAT_GORUNTULE'
  | 'SEVKIYAT_OLUSTUR'
  | 'SEVKIYAT_IPTAL'
  | 'CIKIS_GIR'
  | 'CIKIS_GORUNTULE'
  | 'SOFOR_BEYAN_GIR'
  | 'SOFOR_BEYAN_GORUNTULE'
  | 'TESLIM_GIR'
  | 'TESLIM_GORUNTULE'
  | 'KANIT_YUKLE'
  | 'KANIT_GORUNTULE'
  | 'KANIT_GECERSIZ_ISARETLE'
  | 'KAYIT_DUZELT'
  | 'ITIRAZ_AC'
  | 'UYUSMAZLIK_GORUNTULE'
  | 'UYUSMAZLIK_KARAR'
  | 'AUDIT_GORUNTULE'
  | 'RAPOR_GORUNTULE';

export type Role = 'ADMIN' | 'DEPO' | 'SOFOR' | 'FASONCU' | 'KALITE' | 'YONETICI';

export type CurrentUser = {
  userId: string;
  fullName: string;
  roles: readonly Role[];
  permissions: readonly Permission[];
  siteIds: readonly string[];
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
