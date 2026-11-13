import { loginUrl, safeReturnTo } from '@iplik/api-client';
import { describe, expect, it } from 'vitest';

describe('loginUrl', () => {
  it('uygulama içi yolu returnTo olarak kodlar', () => {
    expect(loginUrl('/saha/sevkiyat/shp-1?x=1')).toBe(
      '/giris?returnTo=%2Fsaha%2Fsevkiyat%2Fshp-1%3Fx%3D1',
    );
  });

  it('kök, dış ve protokol-göreli adreslerde returnTo eklemez', () => {
    const plain = '/giris';
    expect(loginUrl('/')).toBe(plain);
    expect(loginUrl('//evil.example')).toBe(plain);
    expect(loginUrl('https://evil.example')).toBe(plain);
  });

  it('safeReturnTo yalnızca uygulama içi yolu kabul eder', () => {
    expect(safeReturnTo('/saha/x?y=1')).toBe('/saha/x?y=1');
    expect(safeReturnTo(null)).toBe('/');
    expect(safeReturnTo('//evil.example')).toBe('/');
    expect(safeReturnTo('https://evil.example')).toBe('/');
  });
});
