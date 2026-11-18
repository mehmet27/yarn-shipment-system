import { defineProject, mergeConfig } from 'vitest/config';
import viteConfig from './vite.config';

// Uzun kullanıcı akışları yavaş (Windows) CI makinesinde 5 sn'yi aşabiliyor; beklenen süre değil, üst sınırdır.
const TEST_TIMEOUT_MS = 20_000;

export default mergeConfig(
  viteConfig,
  defineProject({
    test: {
      name: 'web',
      environment: 'jsdom',
      setupFiles: ['./src/test/setup.ts'],
      testTimeout: TEST_TIMEOUT_MS,
    },
  }),
);
