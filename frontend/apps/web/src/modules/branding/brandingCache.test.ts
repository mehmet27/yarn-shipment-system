import { DEFAULT_BRAND_COLOR } from '@iplik/ui';
import { afterEach, describe, expect, it } from 'vitest';
import { loadCachedBranding, saveCachedBranding } from './brandingCache';

const KEY = 'yarnshipment.branding';

describe('brandingCache', () => {
  afterEach(() => {
    window.localStorage.clear();
  });

  it('kaydedilen marka ayarını geri okur', () => {
    saveCachedBranding({ primaryColor: DEFAULT_BRAND_COLOR, companyName: 'Gencer Fabric' });

    expect(loadCachedBranding()).toEqual({
      primaryColor: DEFAULT_BRAND_COLOR,
      companyName: 'Gencer Fabric',
    });
  });

  it('kayıt yoksa ya da bozuksa varsayılana düşer', () => {
    expect(loadCachedBranding()).toBeUndefined();

    window.localStorage.setItem(KEY, '{bozuk');
    expect(loadCachedBranding()).toBeUndefined();

    window.localStorage.setItem(KEY, JSON.stringify({ primaryColor: 'mavi' }));
    expect(loadCachedBranding()).toBeUndefined();

    window.localStorage.setItem(
      KEY,
      JSON.stringify({ primaryColor: DEFAULT_BRAND_COLOR, companyName: 5 }),
    );
    expect(loadCachedBranding()).toBeUndefined();
  });
});
