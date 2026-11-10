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
export const LOGIN_PATH = '/oauth2/authorization/keycloak';

/**
 * Giriş adresi; gateway girişten sonra `returnTo` ile verilen uygulama içi yola döner. Yol göreli
 * olmalıdır (`/` ile başlar, `//` ile değil); aksi halde kök kullanılır, gateway zaten reddeder.
 */
export function loginUrl(returnTo: string): string {
  const isLocalPath = returnTo.startsWith('/') && !returnTo.startsWith('//');
  return isLocalPath && returnTo !== '/'
    ? `${LOGIN_PATH}?returnTo=${encodeURIComponent(returnTo)}`
    : LOGIN_PATH;
}

/**
 * Şifre değiştirme adresi: gateway girişi Keycloak'ın kendi "şifre değiştir" eylemiyle başlatır (`kc_action`). Şifre
 * bu uygulamadan hiç geçmez; işlem bitince kişi uygulamaya döner.
 */
export function changePasswordUrl(): string {
  return `${LOGIN_PATH}?kc_action=UPDATE_PASSWORD`;
}

/** Üretilen yanıtı genişletilmiş izin tipine taşır (her üretilen izin geçerli bir `Permission`'dır). */
export async function getCurrentUser(): Promise<CurrentUser> {
  return getGeneratedCurrentUser();
}

/**
 * Çıkış gateway'de; CSRF başlığıyla POST. Yanıttaki `logoutUrl` Keycloak oturumunu da kapatır,
 * tarayıcı oraya gitmezse sonraki giriş sessizce geçer. Adres yoksa giriş sayfasına dönülür.
 */
export async function logout(): Promise<string> {
  const { data } = await httpClient.post<unknown>('/logout');
  const isLogoutBody = typeof data === 'object' && data !== null && 'logoutUrl' in data;
  return isLogoutBody && typeof data.logoutUrl === 'string' ? data.logoutUrl : LOGIN_PATH;
}
