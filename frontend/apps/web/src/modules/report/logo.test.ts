import { afterEach, describe, expect, it, vi } from 'vitest';
import { isLogoType, loadLogo, saveLogo } from './logo';

describe('rapor logosu', () => {
  afterEach(() => {
    window.localStorage.clear();
    vi.restoreAllMocks();
  });

  it('yalnızca PNG ve JPEG kabul edilir', () => {
    expect(isLogoType('image/png')).toBe(true);
    expect(isLogoType('image/jpeg')).toBe(true);
    expect(isLogoType('image/svg+xml')).toBe(false);
    expect(isLogoType('application/pdf')).toBe(false);
  });

  it('kaydedilen logo geri okunur, null kaydı siler', () => {
    expect(saveLogo('data:image/png;base64,AAAA')).toBe(true);
    expect(loadLogo()).toBe('data:image/png;base64,AAAA');
    expect(saveLogo(null)).toBe(true);
    expect(loadLogo()).toBeNull();
  });

  it('görsel olmayan saklanmış değeri yok sayar', () => {
    window.localStorage.setItem('iplik.report.logo', 'javascript:alert(1)');
    expect(loadLogo()).toBeNull();
  });

  it('depolama kullanılamıyorsa hata fırlatmaz', () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('kota');
    });
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('erişim yok');
    });
    expect(saveLogo('data:image/png;base64,AAAA')).toBe(false);
    expect(loadLogo()).toBeNull();
  });
});
