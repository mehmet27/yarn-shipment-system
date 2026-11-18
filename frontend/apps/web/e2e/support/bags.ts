import { expect, type Page } from '@playwright/test';

const BAG_KG = '991,25';

/** Çuvallar sekmesinde iki çuval ekler; toplam 1.982,5 kg (2.000 − 17,5 tartısına uyar). */
export async function addBags(page: Page): Promise<void> {
  await page.getByRole('tab', { name: /Çuvallar/ }).click();
  for (let i = 0; i < 2; i += 1) {
    await page.getByLabel(/Çuval kilosu/).fill(BAG_KG);
    await page.getByRole('button', { name: 'Listeye ekle' }).click();
  }
  await page.getByRole('button', { name: /çuvalı ekle/ }).click();
  await expect(page.getByText('Sevkiyattaki çuvallar (2)')).toBeVisible();
}
