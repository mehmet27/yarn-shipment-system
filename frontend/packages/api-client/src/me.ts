import type { CurrentUserResponse, Permission, Role } from './generated/shipmentService';
import { getCurrentUser } from './generated/shipmentService';
import { httpClient } from './httpClient';

export type CurrentUser = CurrentUserResponse;
export type { Permission, Role };

export const ME_PATH = '/api/v1/me';
export const LOGIN_PATH = '/oauth2/authorization/keycloak';

export { getCurrentUser };

/** Çıkış gateway'de; CSRF başlığıyla POST (yetkilendirme analizi). */
export async function logout(): Promise<void> {
  await httpClient.post('/logout');
}
