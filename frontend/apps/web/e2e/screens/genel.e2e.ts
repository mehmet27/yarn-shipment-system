import { expect, test } from '../support/test';

test.describe('Genel', { tag: '@panel' }, () => {
  // H-25: bir sayfa çizilirken hata olursa beyaz ekran değil, yeniden deneme sayfası görünür.
  test('çöken sayfada yeniden deneme ekranı çıkar, yeniden dene düzeltir', async ({
    page,
    openAs,
    browserErrors,
  }) => {
    // İlk açılışta kilolar tablosu her çizimde patlar (React yeniden denese de); "Yeniden dene"
    // sayfayı yenileyince düzelir. Bayrak, yenilemede de kalan window.name'dedir.
    await page.addInitScript(() => {
      const isFirstLoad = window.name !== 'e2e-crashed';
      window.name = 'e2e-crashed';
      const crashOnFirstLoad = (value: unknown) => {
        if (isFirstLoad && String(value) === '1982.500') {
          throw new Error('e2e: geçici çizim hatası');
        }
      };
      const OriginalFormat = Intl.NumberFormat;
      type FormatInput = Parameters<Intl.NumberFormat['format']>[0];
      function PatchedFormat(...args: ConstructorParameters<typeof Intl.NumberFormat>) {
        const formatter = new OriginalFormat(...args);
        return new Proxy(formatter, {
          get(target, property) {
            if (property !== 'format') {
              const member: unknown = Reflect.get(target, property, target);
              if (typeof member !== 'function') return member;
              return (...rest: unknown[]) => {
                const result: unknown = Reflect.apply(member, target, rest);
                return result;
              };
            }
            return (value: FormatInput) => {
              crashOnFirstLoad(value);
              return target.format(value);
            };
          },
        });
      }
      Object.defineProperty(Intl, 'NumberFormat', { value: PatchedFormat });
    });
    await openAs('QUALITY', '/panel/sevkiyat/shp-0007');
    await expect(page.getByRole('alert')).toContainText(
      'Sayfa yüklenemedi. Bağlantınızı kontrol edip yeniden deneyin.',
    );
    await page.getByRole('button', { name: 'Yeniden dene' }).click();
    await expect(page.getByRole('cell', { name: '1.982,5 kg' })).toBeVisible();
    // Bilerek üretilen hata; başka hata olmamalı.
    expect(browserErrors.every((error) => error.includes('geçici çizim hatası'))).toBe(true);
    browserErrors.length = 0;
  });

  // H-29: sayfa simgesi tanımlı ve var; tarayıcı /favicon.ico için 404 almaz.
  test('sayfa simgesi tanımlı ve yüklenir', async ({ page, openAs }) => {
    await openAs('WAREHOUSE', '/panel/sevkiyat');
    const href = await page.locator('link[rel="icon"]').getAttribute('href');
    expect(href).toBe('/favicon.svg');
    const response = await page.request.get(href ?? '');
    expect(response.status()).toBe(200);
  });
});
