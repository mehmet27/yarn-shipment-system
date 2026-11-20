import type { BarListItem } from '@iplik/ui';
import { sumSignedKg } from '@iplik/units';

/** Çubuk listesinde ayrı ayrı gösterilen en çok satır; fazlası tek "Diğer" satırında toplanır. */
export const MAX_BARS = 8;

export type KgEntry = { key: string; label: string; kg: string; note?: string | undefined };

/** Kiloya göre büyükten küçüğe; eşitlikte ada göre (Türkçe sıralama). */
export function sortByKgDesc<T extends KgEntry>(entries: readonly T[]): T[] {
  const collator = new Intl.Collator('tr-TR');
  return [...entries].sort(
    (a, b) => Number(b.kg) - Number(a.kg) || collator.compare(a.label, b.label),
  );
}

/**
 * En büyük {@link MAX_BARS} satır ayrı, kalanı `otherLabel` adlı tek satırda toplanır; böylece çubukların toplamı
 * tablonun toplamıyla aynı kalır. `otherLabel` kalan satır sayısını alır.
 */
export function foldOthers(
  entries: readonly KgEntry[],
  otherLabel: (count: number) => string,
): KgEntry[] {
  const sorted = sortByKgDesc(entries);
  if (sorted.length <= MAX_BARS + 1) return sorted;
  const rest = sorted.slice(MAX_BARS);
  return [
    ...sorted.slice(0, MAX_BARS),
    {
      key: 'other',
      label: otherLabel(rest.length),
      kg: sumSignedKg(rest.map((entry) => entry.kg)),
    },
  ];
}

/** Kilo satırlarını çubuk listesine çevirir; eksi kilo çubuk çizmez ve dikkat rengiyle yazılır. */
export function toKgBars(
  entries: readonly KgEntry[],
  formatKg: (kg: string) => string,
): BarListItem[] {
  return entries.map((entry) => ({
    key: entry.key,
    label: entry.label,
    value: Number(entry.kg),
    valueLabel: formatKg(entry.kg),
    note: entry.note,
    tone: entry.kg.startsWith('-') ? 'danger' : 'neutral',
  }));
}
