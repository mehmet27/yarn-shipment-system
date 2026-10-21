import type { CurrentUserResponse, Permission, Role } from './generated/shipmentService';
import { getCurrentUser } from './generated/shipmentService';
import { httpClient } from './httpClient';

export type CurrentUser = CurrentUserResponse;
export type { Permission, Role };

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

export { getCurrentUser };

/**
 * Çıkış gateway'de; CSRF başlığıyla POST. Yanıttaki `logoutUrl` Keycloak oturumunu da kapatır,
 * tarayıcı oraya gitmezse sonraki giriş sessizce geçer. Adres yoksa giriş sayfasına dönülür.
 */
export async function logout(): Promise<string> {
  const { data } = await httpClient.post<unknown>('/logout');
  const isLogoutBody = typeof data === 'object' && data !== null && 'logoutUrl' in data;
  return isLogoutBody && typeof data.logoutUrl === 'string' ? data.logoutUrl : LOGIN_PATH;
}
