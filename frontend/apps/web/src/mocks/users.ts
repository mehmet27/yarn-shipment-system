import type { CurrentUser, Permission, Role } from '@iplik/api-client';

/** Yetkilendirme analizi rol-izin matrisinin sahte API karşılığı ("kendi" kısıtı backend'dedir). */
const PERMISSIONS_BY_ROLE: Readonly<Record<Role, readonly Permission[]>> = {
  ADMIN: [
    'TANIM_GORUNTULE',
    'TANIM_YONET',
    'KULLANICI_YONET',
    'AYAR_YONET',
    'PARTI_KALITE_BELIRLE',
    'SEVKIYAT_GORUNTULE',
    'SEVKIYAT_OLUSTUR',
    'SEVKIYAT_IPTAL',
    'CIKIS_GORUNTULE',
    'SOFOR_BEYAN_GORUNTULE',
    'TESLIM_GORUNTULE',
    'KANIT_GORUNTULE',
    'KANIT_GECERSIZ_ISARETLE',
    'KAYIT_DUZELT',
    'ITIRAZ_AC',
    'UYUSMAZLIK_GORUNTULE',
    'UYUSMAZLIK_KARAR',
    'AUDIT_GORUNTULE',
    'RAPOR_GORUNTULE',
  ],
  DEPO: [
    'TANIM_GORUNTULE',
    'SEVKIYAT_GORUNTULE',
    'SEVKIYAT_OLUSTUR',
    'CIKIS_GIR',
    'CIKIS_GORUNTULE',
    'SOFOR_BEYAN_GORUNTULE',
    'TESLIM_GORUNTULE',
    'KANIT_YUKLE',
    'KANIT_GORUNTULE',
  ],
  SOFOR: [
    'SEVKIYAT_GORUNTULE',
    'CIKIS_GORUNTULE',
    'SOFOR_BEYAN_GIR',
    'SOFOR_BEYAN_GORUNTULE',
    'KANIT_YUKLE',
    'KANIT_GORUNTULE',
  ],
  FASONCU: [
    'SEVKIYAT_GORUNTULE',
    'CIKIS_GORUNTULE',
    'TESLIM_GIR',
    'TESLIM_GORUNTULE',
    'KANIT_YUKLE',
    'KANIT_GORUNTULE',
    'ITIRAZ_AC',
    'UYUSMAZLIK_GORUNTULE',
  ],
  KALITE: [
    'TANIM_GORUNTULE',
    'PARTI_KALITE_BELIRLE',
    'SEVKIYAT_GORUNTULE',
    'CIKIS_GORUNTULE',
    'SOFOR_BEYAN_GORUNTULE',
    'TESLIM_GORUNTULE',
    'KANIT_YUKLE',
    'KANIT_GORUNTULE',
    'ITIRAZ_AC',
    'UYUSMAZLIK_GORUNTULE',
    'UYUSMAZLIK_KARAR',
    'AUDIT_GORUNTULE',
    'RAPOR_GORUNTULE',
  ],
  YONETICI: [
    'TANIM_GORUNTULE',
    'SEVKIYAT_GORUNTULE',
    'CIKIS_GORUNTULE',
    'SOFOR_BEYAN_GORUNTULE',
    'TESLIM_GORUNTULE',
    'KANIT_GORUNTULE',
    'UYUSMAZLIK_GORUNTULE',
    'AUDIT_GORUNTULE',
    'RAPOR_GORUNTULE',
  ],
};

const FULL_NAMES: Readonly<Record<Role, string>> = {
  ADMIN: 'Ayşe Yönetici',
  DEPO: 'Deniz Depo',
  SOFOR: 'Şahin Şoför',
  FASONCU: 'Fatma Fasoncu',
  KALITE: 'Kemal Kalite',
  YONETICI: 'Yusuf Müdür',
};

export const MOCK_ROLES = Object.keys(PERMISSIONS_BY_ROLE).filter(
  (key): key is Role => key in PERMISSIONS_BY_ROLE,
);

export function createMockUser(role: Role): CurrentUser {
  return {
    userId: `user-${role.toLowerCase()}`,
    fullName: FULL_NAMES[role],
    roles: [role],
    permissions: PERMISSIONS_BY_ROLE[role],
    siteIds: ['site-merkez'],
  };
}
