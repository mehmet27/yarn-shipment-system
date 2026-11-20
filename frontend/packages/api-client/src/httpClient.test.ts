import type { InternalAxiosRequestConfig } from 'axios';
import { describe, expect, it } from 'vitest';
import { httpClient, READ_TIMEOUT_MS } from './httpClient';

/** İsteği ağa göndermeden, interceptor'lardan geçmiş ayarı döndürür. */
async function sentConfig(method: string, timeout?: number): Promise<InternalAxiosRequestConfig> {
  let seen: InternalAxiosRequestConfig | undefined;
  await httpClient.request({
    url: '/api/v1/x',
    method,
    ...(timeout === undefined ? {} : { timeout }),
    adapter: (config) => {
      seen = config;
      return Promise.resolve({ data: null, status: 200, statusText: 'OK', headers: {}, config });
    },
  });
  if (seen === undefined) throw new Error('istek gönderilmedi');
  return seen;
}

describe('httpClient', () => {
  it('okuma isteği süre sınırıyla gider; asılı kalan istek ekranı sonsuza dek bekletmez', async () => {
    expect((await sentConfig('get')).timeout).toBe(READ_TIMEOUT_MS);
  });

  it('çağıranın verdiği süre sınırına dokunulmaz', async () => {
    expect((await sentConfig('get', 5_000)).timeout).toBe(5_000);
  });

  it('yazma isteğine süre sınırı konmaz ve Idempotency-Key eklenir', async () => {
    const config = await sentConfig('post');
    expect(config.timeout ?? 0).toBe(0);
    expect(config.headers.get('Idempotency-Key')).toBeTruthy();
  });
});
