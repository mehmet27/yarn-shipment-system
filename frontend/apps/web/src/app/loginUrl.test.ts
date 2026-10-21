import { loginUrl } from '@iplik/api-client';
import { describe, expect, it } from 'vitest';

describe('loginUrl', () => {
  it('uygulama içi yolu returnTo olarak kodlar', () => {
    expect(loginUrl('/saha/sevkiyat/shp-1?x=1')).toBe(
      '/oauth2/authorization/keycloak?returnTo=%2Fsaha%2Fsevkiyat%2Fshp-1%3Fx%3D1',
    );
  });

  it('kök, dış ve protokol-göreli adreslerde returnTo eklemez', () => {
    const plain = '/oauth2/authorization/keycloak';
    expect(loginUrl('/')).toBe(plain);
    expect(loginUrl('//evil.example')).toBe(plain);
    expect(loginUrl('https://evil.example')).toBe(plain);
  });
});
