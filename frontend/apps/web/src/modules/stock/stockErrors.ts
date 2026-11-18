import { toProblem } from '@iplik/api-client';

/** Geçersiz giriş ya da yetersiz stok (`INVALID_LEDGER_INPUT`) ayrı mesaj alır, kalanı genel mesaja düşer. */
export function stockErrorKey(error: unknown): string {
  const { code } = toProblem(error);
  if (code === 'INVALID_LEDGER_INPUT') return 'stock.errors.invalid';
  if (code === 'YARN_LOT_GRADE_REQUIRED') return 'stock.errors.lotGradeRequired';
  if (code === 'YARN_LOT_OTHER_YARN') return 'stock.errors.lotOtherYarn';
  if (code === 'YARN_LOT_OTHER_SUPPLIER') return 'stock.errors.lotOtherSupplier';
  if (code === 'YARN_LOT_NO_SITE') return 'stock.errors.lotNoSite';
  return code === 'INSUFFICIENT_CENTRAL_STOCK'
    ? 'stock.errors.insufficient'
    : 'stock.errors.unknown';
}
