/** FE-SEC-09: import.meta.env yalnızca bu dosyada okunur. */
export function isMockApiEnabled(): boolean {
  return import.meta.env.DEV && import.meta.env.VITE_USE_MOCK_API !== 'false';
}
