import { logout, LOGIN_PATH } from '@iplik/api-client';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { server } from '@/mocks/server';

describe('logout', () => {
  it('gateway yanıtındaki Keycloak çıkış adresini döner', async () => {
    server.use(
      http.post('/logout', () => HttpResponse.json({ logoutUrl: 'http://kc/logout?x=1' })),
    );
    expect(await logout()).toBe('http://kc/logout?x=1');
  });

  it('adres yoksa giriş sayfasına döner', async () => {
    server.use(http.post('/logout', () => new HttpResponse(null, { status: 204 })));
    expect(await logout()).toBe(LOGIN_PATH);
  });
});
