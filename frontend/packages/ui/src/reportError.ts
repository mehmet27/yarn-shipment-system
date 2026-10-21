/**
 * FE-STRUC-07: tek hata bildirme noktası. Sentry uyumlu sunucu (F-11) bağlanınca
 * burası değişir; kişisel veri gönderilmez (FE-SEC-08).
 */
export function reportError(error: unknown): void {
  globalThis.reportError(error);
}
