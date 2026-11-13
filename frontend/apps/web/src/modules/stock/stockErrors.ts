import { toProblem } from '@iplik/api-client';

/** Geçersiz giriş ya da yetersiz stok (`INVALID_LEDGER_INPUT`) ayrı mesaj alır, kalanı genel mesaja düşer. */
export function stockErrorKey(error: unknown): string {
  const { code } = toProblem(error);
  if (code === 'INVALID_LEDGER_INPUT') return 'stock.errors.invalid';
  return code === 'INSUFFICIENT_CENTRAL_STOCK' ? 'stock.errors.insufficient' : 'stock.errors.unknown';
}
