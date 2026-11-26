import type { LotView } from '@iplik/api-client';

/** `unknown`: liste henüz gelmedi ya da iplik/tedarikçi seçilmedi; kalite sınıfı bu durumda da sorulur. */
export type LotState = 'none' | 'unknown' | 'existing' | 'conflict' | 'new';

type Owner = { productId: string | undefined; supplierId: string | undefined };
type Lot = Pick<LotView, 'lotNo' | 'productId' | 'yarnSupplierId'>;

/** Sunucudaki kuralın aynısı: parti aynı ipliğin olmalı; tedarikçisi kayıtlıysa o da tutmalı. */
function fits(lot: Lot, { productId, supplierId }: Owner): boolean {
  return (
    lot.productId === productId &&
    (lot.yarnSupplierId === undefined || lot.yarnSupplierId === supplierId)
  );
}

/** Seçili iplik ve tedarikçinin parti numaraları; ikisi de seçilmeden liste boştur. */
export function lotSuggestions(lots: readonly Lot[], owner: Owner): string[] {
  if (owner.productId === undefined || owner.supplierId === undefined) return [];
  return [...new Set(lots.filter((lot) => fits(lot, owner)).map((lot) => lot.lotNo))];
}

/** Yazılan numaranın durumu; `lots` yüklenmediyse `undefined` verilir. */
export function lotState(typed: string, lots: readonly Lot[] | undefined, owner: Owner): LotState {
  const lotNo = typed.trim().toUpperCase();
  if (lotNo === '') return 'none';
  if (lots === undefined) return 'unknown';
  const same = lots.filter((lot) => lot.lotNo.toUpperCase() === lotNo);
  if (same.length === 0) return 'new';
  if (owner.productId === undefined || owner.supplierId === undefined) return 'unknown';
  return same.some((lot) => fits(lot, owner)) ? 'existing' : 'conflict';
}
