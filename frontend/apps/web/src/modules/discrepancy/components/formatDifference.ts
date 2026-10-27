import { formatKg, formatPct, isKilogram } from '@iplik/units';

function formatKgText(diffKg: string): string {
  return isKilogram(diffKg) ? formatKg(diffKg) : diffKg;
}

/** Fark kg ve yüzde olarak; sunucu değeri kilogram biçiminde değilse olduğu gibi gösterilir. */
export function formatDifference(diffKg: string | undefined, diffPct: string | undefined): string {
  const kg = diffKg === undefined ? undefined : formatKgText(diffKg);
  const pct = diffPct === undefined ? undefined : formatPct(diffPct);
  return [kg, pct].filter((part) => part !== undefined).join(' · ');
}
