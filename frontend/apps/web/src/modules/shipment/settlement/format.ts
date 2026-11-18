import { formatKgText, formatPct, rateToPct } from '@iplik/units';

export { formatKgText };

/** İşaretli hareket kilosu: "-100.000" → "−100 kg", "+" işareti yok. */
export function formatSignedKg(value: string): string {
  return value.startsWith('-') ? `−${formatKgText(value.slice(1))}` : formatKgText(value);
}

/** "4.03" → "%4,03". */
export function formatPercentText(value: string): string {
  return /^-?\d+(\.\d+)?$/.test(value) ? formatPct(value) : value;
}

/** Fire oranı "0.0400" → "%4". */
export function formatFireRate(rate: string): string {
  const pct = rateToPct(rate);
  return pct === undefined ? rate : formatPercentText(pct);
}
