// Robot testleri: gerçek arayüz ve gerçek backend'e karşı çalışır. Kullanım ve kurallar: robot/README.md
import { defineConfig } from '@playwright/test';

const kanal = process.env.ROBOT_TARAYICI ?? (process.platform === 'win32' ? 'msedge' : 'chromium');
const izle = process.env.ROBOT_IZLE === '1';
const yavas = Number(process.env.ROBOT_YAVAS ?? (izle ? '250' : '0'));

export default defineConfig({
  testDir: './senaryolar',
  testMatch: '**/*.robot.ts',
  // Senaryolar yerel veritabanına kayıt yazar; birbirini beklemeden değil sırayla koşar.
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 300_000,
  expect: { timeout: 8_000 },
  outputDir: './sonuclar',
  reporter: [['list'], ['./support/rapor.ts']],
  use: {
    baseURL: process.env.ROBOT_URL ?? 'http://localhost:5173',
    locale: 'tr-TR',
    timezoneId: 'Europe/Istanbul',
    actionTimeout: 15_000,
    navigationTimeout: 30_000,
    headless: !izle,
    viewport: { width: 1280, height: 900 },
    video: process.env.ROBOT_VIDEO === '1' ? 'on' : 'off',
    trace: 'off',
    ...(kanal === 'chromium' ? {} : { channel: kanal }),
    launchOptions: { slowMo: yavas },
  },
});
