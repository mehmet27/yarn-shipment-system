/** Uçtan uca test paketi `vite build --mode e2e` ile derlenir (e2e/README.md). */
const E2E_MODE = 'e2e';

/**
 * FE-SEC-09: import.meta.env yalnızca bu dosyada okunur. Sahte API geliştirme sunucusunda ya da
 * açıkça e2e kipinde derlenen test paketinde açılır; üretim derlemesinde (mode=production) hiçbir
 * koşulda açılmaz.
 */
export function isMockApiEnabled(): boolean {
  if (import.meta.env.MODE === E2E_MODE) return true;
  return import.meta.env.DEV && import.meta.env.VITE_USE_MOCK_API !== 'false';
}
