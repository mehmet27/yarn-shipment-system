import type {
  ChangeLoginAccountEnabledRequest,
  ChangeLoginAccountRoleRequest,
  Permission,
  SetLoginAccountPasswordRequest,
  StaffLoginAccountRequest,
} from '@iplik/api-client';
import { http, HttpResponse } from 'msw';
import {
  addMockLoginAccount,
  changeMockLoginAccountRole,
  isMockUsernameTaken,
  listMockLoginAccounts,
  setMockLoginAccountEnabled,
  setMockLoginAccountPassword,
} from './loginAccounts';

const PATH = '/api/v1/master-data/login-accounts';
const DEFAULT_PAGE_SIZE = 20;

/** Arama giriş adı ve adda; büyük-küçük harf duyarsız (Türkçe yerel ayarla). */
function matches(text: string | undefined, q: string): boolean {
  return text?.toLocaleLowerCase('tr').includes(q) ?? false;
}

function problem(status: number, code: string, title: string) {
  return HttpResponse.json({ status, code, title }, { status });
}

/** Giriş hesabı listesi, fabrika içi hesap açma, rol değiştirme, pasife alma ve yeni geçici şifre verme; yalnızca USER_MANAGE (yönetici), aksi halde 403. */
export function createLoginAccountHandlers(permissions: readonly Permission[]) {
  const canList = permissions.includes('USER_MANAGE');
  return [
    http.post(PATH, async ({ request }) => {
      if (!canList) return problem(403, 'FORBIDDEN', 'Yetkiniz yok');
      const body = (await request.json()) as StaffLoginAccountRequest;
      if (isMockUsernameTaken(body.username)) {
        return problem(409, 'LOGIN_ACCOUNT_EXISTS', 'Giriş adı alınmış');
      }
      addMockLoginAccount({
        username: body.username,
        fullName: body.fullName,
        role: body.role,
        workArea: body.workArea,
      });
      return new HttpResponse(null, { status: 201 });
    }),
    http.put(`${PATH}/:id/role`, async ({ params, request }) => {
      if (!canList) return problem(403, 'FORBIDDEN', 'Yetkiniz yok');
      const body = (await request.json()) as ChangeLoginAccountRoleRequest;
      const result = changeMockLoginAccountRole(String(params.id), body.role, body.workArea);
      if (result === 'not-found') {
        return problem(404, 'LOGIN_ACCOUNT_NOT_FOUND', 'Kullanıcı yok');
      }
      return new HttpResponse(null, { status: 204 });
    }),
    http.put(`${PATH}/:id/enabled`, async ({ params, request }) => {
      if (!canList) return problem(403, 'FORBIDDEN', 'Yetkiniz yok');
      const body = (await request.json()) as ChangeLoginAccountEnabledRequest;
      if (!setMockLoginAccountEnabled(String(params.id), body.enabled)) {
        return problem(404, 'LOGIN_ACCOUNT_NOT_FOUND', 'Kullanıcı yok');
      }
      return new HttpResponse(null, { status: 204 });
    }),
    http.put(`${PATH}/:id/password`, async ({ params, request }) => {
      if (!canList) return problem(403, 'FORBIDDEN', 'Yetkiniz yok');
      const body = (await request.json()) as SetLoginAccountPasswordRequest;
      if (body.password.length < 8) return problem(400, 'WEAK_PASSWORD', 'Şifre zayıf');
      if (!setMockLoginAccountPassword(String(params.id))) {
        return problem(404, 'LOGIN_ACCOUNT_NOT_FOUND', 'Kullanıcı yok');
      }
      return new HttpResponse(null, { status: 204 });
    }),
    http.get(PATH, ({ request }) => {
      if (!canList) return problem(403, 'FORBIDDEN', 'Yetkiniz yok');
      const params = new URL(request.url).searchParams;
      const page = Number(params.get('page') ?? '0');
      const size = Number(params.get('size') ?? String(DEFAULT_PAGE_SIZE));
      const q = params.get('q')?.trim().toLocaleLowerCase('tr') ?? '';
      const matching = listMockLoginAccounts().filter(
        (account) => q === '' || matches(account.username, q) || matches(account.fullName, q),
      );
      return HttpResponse.json({
        items: matching.slice(page * size, (page + 1) * size),
        page: {
          number: page,
          size,
          totalElements: matching.length,
          totalPages: Math.ceil(matching.length / size),
        },
      });
    }),
  ];
}
