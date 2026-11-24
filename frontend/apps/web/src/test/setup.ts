import '@testing-library/jest-dom/vitest';
import { initI18n } from '@iplik/i18n';
// eslint-disable-next-line testing-library/no-manual-cleanup -- Vitest globals kapalı, otomatik temizlik yok
import { cleanup, configure } from '@testing-library/react';
import { afterAll, afterEach, beforeAll } from 'vitest';
import { resetMockStore } from '../mocks/mockStore';
import { server } from '../mocks/server';

// Dosyanın ilk testi ekranı ilk kez yükler; yavaş (Windows) CI makinesinde bu, varsayılan 1 sn'lik beklemeyi aşabiliyor.
// Beklenen süre değil, üst sınırdır.
configure({ asyncUtilTimeout: 5_000 });

// jsdom'da olmayan tarayıcı API'leri; Mantine bunları kullanır.
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string) => ({
    // Testler masaüstü genişliğinde çalışır; dar ekran testleri matchMedia'yı kendisi değiştirir.
    matches: query.includes('min-width'),
    media: query,
    onchange: null,
    addListener: () => undefined,
    removeListener: () => undefined,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
    dispatchEvent: () => false,
  }),
});
Element.prototype.scrollIntoView = () => undefined;
window.ResizeObserver = class {
  observe = () => undefined;
  unobserve = () => undefined;
  disconnect = () => undefined;
};

beforeAll(async () => {
  server.listen({ onUnhandledRequest: 'error' }); // FE-TEST-04
  await initI18n();
});
afterEach(() => {
  cleanup();
  server.resetHandlers();
  resetMockStore();
  // Hatırlanan görünüm (son sekme, süzgeç) bir testten ötekine taşmasın.
  window.localStorage.clear();
});
afterAll(() => {
  server.close();
});
