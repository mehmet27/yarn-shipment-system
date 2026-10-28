// Uçtan uca testler (FE-TEST-08). Çalıştırma ve yeni ekran rehberi: e2e/README.md
import { defineConfig, devices } from '@playwright/test';

const PORT = Number(process.env.E2E_PORT ?? '4173');
const BASE_URL = `http://localhost:${String(PORT)}`;
const isCi = process.env.CI !== undefined;
const chromiumPath = process.env.CHROMIUM_PATH;

/**
 * FE-UI-07 genişlikleri. Ekran denetimleri (@ekran) üç genişlikte de koşar; akışlar kendi
 * cihazında koşar: saha (@saha) telefonda, yönetim paneli (@panel) masaüstünde.
 */
const VIEWPORTS = [
  { name: 'telefon-360', width: 360, height: 780, hasTouch: true, grep: /@ekran|@saha/ },
  { name: 'tablet-768', width: 768, height: 1024, hasTouch: true, grep: /@ekran/ },
  { name: 'masaustu-1280', width: 1280, height: 800, hasTouch: false, grep: /@ekran|@panel/ },
] as const;

export default defineConfig({
  testDir: '.',
  testMatch: '**/*.e2e.ts',
  // Sahte API tarayıcıdadır ve her sayfa açılışında sıfırlanır; testler birbirini beklemez.
  fullyParallel: true,
  forbidOnly: isCi,
  retries: isCi ? 1 : 0,
  timeout: 20_000,
  expect: { timeout: 5_000 },
  reporter: isCi
    ? [['github'], ['html', { open: 'never', outputFolder: 'rapor' }]]
    : [['list'], ['html', { open: 'never', outputFolder: 'rapor' }]],
  outputDir: 'sonuclar',
  use: {
    baseURL: BASE_URL,
    locale: 'tr-TR',
    timezoneId: 'Europe/Istanbul',
    // İz kaydı testi yavaşlatır; yerelde hata ayıklarken --trace on ile açılır.
    trace: isCi ? 'retain-on-failure' : 'off',
    screenshot: 'only-on-failure',
    ...(chromiumPath === undefined ? {} : { launchOptions: { executablePath: chromiumPath } }),
  },
  projects: VIEWPORTS.map(({ name, width, height, hasTouch, grep }) => ({
    name,
    grep,
    use: { ...devices['Desktop Chrome'], viewport: { width, height }, hasTouch },
  })),
  webServer: {
    // Sahte API'li test paketi (mode=e2e) derlenip önizleme sunucusunda açılır. Geliştirme
    // sunucusuna göre sayfa başına modül sayısı çok az olduğu için testler birkaç kat hızlıdır.
    command: `pnpm exec vite build --mode e2e --outDir dist-e2e --logLevel warn && pnpm exec vite preview --outDir dist-e2e --port ${String(PORT)} --strictPort`,
    cwd: '..',
    url: BASE_URL,
    reuseExistingServer: !isCi,
    timeout: 60_000,
  },
});
