import { defineConfig } from 'vitest/config';

/** FE-TEST-07: kapsam eşikleri; altına düşmek CI'ı kırar. */
export default defineConfig({
  test: {
    projects: ['apps/*', { test: { name: 'packages', include: ['packages/*/src/**/*.test.ts'] } }],
    coverage: {
      provider: 'v8',
      include: ['apps/*/src/**', 'packages/*/src/**'],
      exclude: ['**/*.test.*', '**/test/**', '**/mocks/**', '**/main.tsx', '**/index.ts'],
      thresholds: {
        lines: 70,
        'packages/units/src/**': { lines: 100, branches: 100 },
        'packages/api-client/src/problem.ts': { lines: 90 },
        'apps/web/src/modules/auth/**': { lines: 90 },
      },
    },
  },
});
