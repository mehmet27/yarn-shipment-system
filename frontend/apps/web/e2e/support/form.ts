import type { Page } from '@playwright/test';

/** Açılır listeden (Mantine Select) seçim yapar. */
export async function pick(page: Page, label: string | RegExp, option: string) {
  await page.getByRole('combobox', { name: label }).click();
  await page.getByRole('option', { name: option, exact: true }).click();
}

/** Etiketine göre alanları sırayla doldurur. */
export async function fillFields(page: Page, values: Record<string, string>) {
  for (const [label, value] of Object.entries(values)) {
    await page.getByLabel(label, { exact: true }).fill(value);
  }
}
