import { toProblem } from '@iplik/api-client';

/** Geçersiz giriş ya da yetersiz stok (`INVALID_LEDGER_INPUT`) ayrı mesaj alır, kalanı genel mesaja düşer. */
export function stockErrorKey(error: unknown): string {
  return toProblem(error).code === 'INVALID_LEDGER_INPUT'
    ? 'stock.errors.invalid'
    : 'stock.errors.unknown';
}
