import { toProblem } from '@iplik/api-client';

const KNOWN_CODES = new Set([
  'LOT_NOT_LOCKED',
  'LABEL_REPRINT_REASON_REQUIRED',
  'BAG_NOT_AVAILABLE',
]);

/** Hata kodunun çeviri anahtarı; bilinmeyen kod genel mesaja düşer (FE-API-05). */
export function bagErrorKey(error: unknown): string {
  const { code } = toProblem(error);
  return KNOWN_CODES.has(code) ? `bag.errors.${code}` : 'bag.errors.unexpected';
}
