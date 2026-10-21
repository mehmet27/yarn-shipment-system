import type { Role } from '@iplik/api-client';
import { ME_PATH } from '@iplik/api-client';
import { http, HttpResponse } from 'msw';
import { createMockUser } from './users';

export function createMeHandler(role: Role) {
  return http.get(ME_PATH, () => HttpResponse.json(createMockUser(role)));
}

export const unauthenticatedMeHandler = http.get(ME_PATH, () =>
  HttpResponse.json(
    { status: 401, code: 'UNAUTHENTICATED', title: 'Oturum açmanız gerekiyor' },
    { status: 401 },
  ),
);

export function createHandlers(role: Role) {
  return [createMeHandler(role)];
}
