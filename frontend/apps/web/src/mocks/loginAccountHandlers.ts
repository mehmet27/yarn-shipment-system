import type { Permission } from '@iplik/api-client';
import { http, HttpResponse } from 'msw';
import { listMockLoginAccounts } from './loginAccounts';

const PATH = '/api/v1/master-data/login-accounts';
const DEFAULT_PAGE_SIZE = 20;

/** Arama giriş adı, ad ve e-postada; büyük-küçük harf duyarsız (Türkçe yerel ayarla). */
function matches(text: string | undefined, q: string): boolean {
  return text?.toLocaleLowerCase('tr').includes(q) ?? false;
}

/** Giriş hesabı listesi; yalnızca USER_MANAGE (yönetici), aksi halde 403. */
export function createLoginAccountHandlers(permissions: readonly Permission[]) {
  const canList = permissions.includes('USER_MANAGE');
  return [
    http.get(PATH, ({ request }) => {
      if (!canList) {
        return HttpResponse.json(
          { status: 403, code: 'FORBIDDEN', title: 'Yetkiniz yok' },
          { status: 403 },
        );
      }
      const params = new URL(request.url).searchParams;
      const page = Number(params.get('page') ?? '0');
      const size = Number(params.get('size') ?? String(DEFAULT_PAGE_SIZE));
      const q = params.get('q')?.trim().toLocaleLowerCase('tr') ?? '';
      const matching = listMockLoginAccounts().filter(
        (account) =>
          q === '' ||
          matches(account.username, q) ||
          matches(account.fullName, q) ||
          matches(account.email, q),
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
