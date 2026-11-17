import { formatPct, formatSignedKg } from '@iplik/units';

/** Fark kg ve yüzde olarak; eksi farkta da "-3 kg" yazar ("-3.000" Türkçede üç bin okunur). */
export function formatDifference(diffKg: string | undefined, diffPct: string | undefined): string {
  const kg = diffKg === undefined ? undefined : formatSignedKg(diffKg);
  const pct = diffPct === undefined ? undefined : formatPct(diffPct);
  return [kg, pct].filter((part) => part !== undefined).join(' · ');
}
