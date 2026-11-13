import type {
  CurrentUserResponse as GeneratedCurrentUser,
  Permission as GeneratedPermission,
  Role,
} from './generated/shipmentService';
import { getCurrentUser as getGeneratedCurrentUser } from './generated/shipmentService';
import { httpClient } from './httpClient';

/**
 * Bakiye, kumaş, iplik dönüşü ve fire izinleri (analiz Bölüm 3.3) OpenAPI'ye eklenene kadar elle
 * tanımlıdır; üretilen `Permission` bunları içerince bu birleşim kaldırılır.
 */
export type Permission =
  | GeneratedPermission
  | 'BALANCE_VIEW'
  | 'FABRIC_RECEIPT_RECORD'
  | 'YARN_RETURN_RECORD'
  | 'FIRE_RATE_EDIT'
  | 'INVOICE_MARK';
export type CurrentUser = Omit<GeneratedCurrentUser, 'permissions'> & {
  permissions: Permission[];
};
export type { Role };

export const ME_PATH = '/api/v1/me';
export const LOGIN_PATH = '/giris';
export const CHANGE_PASSWORD_PATH = '/sifre-degistir';

/**
 * Giriş sayfası adresi; girişten sonra `returnTo` ile verilen uygulama içi yola dönülür. Yol göreli olmalıdır
 * (`/` ile başlar, `//` ile değil); aksi halde kök kullanılır.
 */
export function loginUrl(returnTo: string): string {
  const isLocalPath = returnTo.startsWith('/') && !returnTo.startsWith('//');
  return isLocalPath && returnTo !== '/'
    ? `${LOGIN_PATH}?returnTo=${encodeURIComponent(returnTo)}`
    : LOGIN_PATH;
}

/** Girişten sonra dönülecek uygulama içi yol; dış ya da protokol-göreli adres kabul edilmez, kök döner. */
export function safeReturnTo(value: string | null): string {
  return value !== null && value.startsWith('/') && !value.startsWith('//') ? value : '/';
}

/** Şifre değiştirme sayfası: kişi kendi şifresini uygulamanın kendi formunda değiştirir. */
export function changePasswordUrl(): string {
  return CHANGE_PASSWORD_PATH;
}

/** Üretilen yanıtı genişletilmiş izin tipine taşır (her üretilen izin geçerli bir `Permission`'dır). */
export async function getCurrentUser(): Promise<CurrentUser> {
  return getGeneratedCurrentUser();
}

/** Oturumu sunucuda kapatır (CSRF başlığıyla POST) ve dönülecek giriş sayfası adresini verir. */
export async function logout(): Promise<string> {
  await httpClient.post('/api/v1/auth/logout');
  return LOGIN_PATH;
}
