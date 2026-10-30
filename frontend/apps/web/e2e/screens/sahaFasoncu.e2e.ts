import { expect, test } from '../support/test';

test.describe('Saha: fasoncu', { tag: '@saha' }, () => {
  // H-15: kör sayım gereği gizli alanlar "Kaydedilmedi" diye yanlış bilgi vermez.
  test('gizli çuval sayısı ve mühür kör kontrol notuyla görünür', async ({ page, openAs }) => {
    await openAs('SUBCONTRACTOR', '/saha/sevkiyat/shp-0004');
    await expect(page.getByText('Teslimde siz gireceksiniz')).toHaveCount(2);
    await expect(page.getByText('Kaydedilmedi')).toHaveCount(0);
    await expect(page.getByText('M-0004')).toHaveCount(0);
  });
});
