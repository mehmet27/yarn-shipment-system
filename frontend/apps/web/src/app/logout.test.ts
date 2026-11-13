import { logout, LOGIN_PATH } from '@iplik/api-client';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { server } from '@/mocks/server';

describe('logout', () => {
  it('oturumu sunucuda kapatır ve giriş sayfası adresini döner', async () => {
    let isCalled = false;
    server.use(
      http.post('/api/v1/auth/logout', () => {
        isCalled = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    expect(await logout()).toBe(LOGIN_PATH);
    expect(isCalled).toBe(true);
  });
});
