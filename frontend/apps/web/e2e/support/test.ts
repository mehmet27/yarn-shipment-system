import { expect, test as base } from '@playwright/test';
import type { Page } from '@playwright/test';

/** Sahte API rolleri (src/mocks/users.ts). */
export type Role = 'ADMIN' | 'WAREHOUSE' | 'DRIVER' | 'SUBCONTRACTOR' | 'QUALITY' | 'MANAGER';

type AppFixtures = {
  /** Sayfayı verilen rolle açar: sahte API rolü ilk açılışta adresten okur. */
  openAs: (role: Role, path: string) => Promise<void>;
  /** Testte görülen konsol hataları ve yakalanmamış istisnalar; test sonunda boş olmalıdır. */
  browserErrors: string[];
};

/** Bilinçli olarak üretilen hatalar (ör. 403/404 yanıtının konsola düşen ağ satırı). */
const EXPECTED_CONSOLE_ERRORS = [
  /Failed to load resource: the server responded with a status of 4\d\d/,
];

function collectErrors(page: Page, errors: string[]) {
  page.on('pageerror', (error) => errors.push(`istisna: ${error.message}`));
  page.on('console', (message) => {
    if (message.type() !== 'error') return;
    const text = message.text();
    if (!EXPECTED_CONSOLE_ERRORS.some((pattern) => pattern.test(text))) {
      errors.push(`konsol: ${text}`);
    }
  });
}

export const test = base.extend<AppFixtures>({
  browserErrors: [
    async ({ page }, use) => {
      const errors: string[] = [];
      collectErrors(page, errors);
      await use(errors);
      expect(errors, 'Tarayıcıda hata oluştu').toEqual([]);
    },
    { auto: true },
  ],
  openAs: async ({ page }, use) => {
    await use(async (role, path) => {
      const separator = path.includes('?') ? '&' : '?';
      await page.goto(`${path}${separator}mockRole=${role}`);
    });
  },
});

export { expect };
