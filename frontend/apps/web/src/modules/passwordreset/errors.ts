const KNOWN_CODES = ['INVALID_RESET_CODE', 'WEAK_PASSWORD', 'SMS_UNAVAILABLE'];

/** Hata koduna karşılık gelen metin anahtarı; bilinmeyen kod genel mesaja düşer. */
export function errorKey(code: string): string {
  return `forgotPassword.errors.${KNOWN_CODES.includes(code) ? code : 'unknown'}`;
}
