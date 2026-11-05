import type { LabeledBagView } from '@iplik/api-client';

export const BAG_FILTERS = ['ALL', 'UNPRINTED', 'PRINTED', 'VOID'] as const;
export type BagFilter = (typeof BAG_FILTERS)[number];

/** Listeyi seçilen süzgece göre daraltır; iptal edilmiş çuvallar yalnızca "İptal" süzgecinde görünür. */
export function filterBags(bags: readonly LabeledBagView[], filter: BagFilter): LabeledBagView[] {
  return bags.filter((item) => matches(item, filter));
}

function matches(item: LabeledBagView, filter: BagFilter): boolean {
  switch (filter) {
    case 'ALL':
      return true;
    case 'UNPRINTED':
      return item.printCount === 0 && item.bag.status !== 'VOID';
    case 'PRINTED':
      return item.printCount > 0;
    case 'VOID':
      return item.bag.status === 'VOID';
  }
}

/** Etiketi hiç basılmamış, basılabilir (depoda ya da sevkiyatta) çuvalların anahtarları. */
export function unprintedKeys(bags: readonly LabeledBagView[]): string[] {
  return bags
    .filter(
      (item) =>
        item.printCount === 0 && (item.bag.status === 'CREATED' || item.bag.status === 'RESERVED'),
    )
    .map((item) => item.bag.uniqueKey);
}
