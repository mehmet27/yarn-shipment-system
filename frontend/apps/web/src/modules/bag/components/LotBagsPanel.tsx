import type { LabeledBagView } from '@iplik/api-client';
import { Stack } from '@iplik/ui';
import { useState } from 'react';
import { BagFilters } from './BagFilters';
import { filterBags, unprintedKeys } from './bagFilter';
import type { BagFilter } from './bagFilter';
import { isPrintable, LotBagTable } from './LotBagTable';
import { PrintLabelsBar } from './PrintLabelsBar';
import { VoidBagsBar } from './VoidBagsBar';

export type LotBagsPanelProps = {
  bags: readonly LabeledBagView[];
  selected: ReadonlySet<string>;
  onSelect: (uniqueKeys: ReadonlySet<string>) => void;
  onPrinted: (pdf: Blob) => void;
};

/** Partinin çuval listesi: baskı ve iptal işlemleri, durum süzgeci ve seçim. */
export function LotBagsPanel({ bags, selected, onSelect, onPrinted }: LotBagsPanelProps) {
  const [filter, setFilter] = useState<BagFilter>('ALL');
  const toggle = (key: string) => {
    const next = new Set(selected);
    if (!next.delete(key)) next.add(key);
    onSelect(next);
  };
  const voidable = voidableKeys(bags, selected);
  return (
    <Stack>
      <PrintLabelsBar
        uniqueKeys={[...selected]}
        reprintCount={reprintCount(bags, selected)}
        onPrinted={onPrinted}
      />
      <VoidBagsBar
        uniqueKeys={voidable}
        hasBlocked={voidable.length < selected.size}
        onVoided={() => {
          onSelect(new Set());
        }}
      />
      <BagFilters
        filter={filter}
        onFilter={setFilter}
        onSelectUnprinted={() => {
          onSelect(new Set(unprintedKeys(bags)));
        }}
      />
      <LotBagTable
        bags={filterBags(bags, filter)}
        selected={selected}
        onToggle={toggle}
        onToggleAll={(keys) => {
          onSelect(new Set(keys));
        }}
      />
    </Stack>
  );
}

function reprintCount(bags: readonly LabeledBagView[], selected: ReadonlySet<string>): number {
  return bags.filter(
    (item) => isPrintable(item) && item.printCount > 0 && selected.has(item.bag.uniqueKey),
  ).length;
}

/** Seçilenlerden yalnızca depodaki çuvallar iptal edilebilir. */
function voidableKeys(bags: readonly LabeledBagView[], selected: ReadonlySet<string>): string[] {
  return bags
    .filter((item) => item.bag.status === 'CREATED' && selected.has(item.bag.uniqueKey))
    .map((item) => item.bag.uniqueKey);
}
