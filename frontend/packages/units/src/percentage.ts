import Big from 'big.js';
import type { Kilogram } from './kilogram';
import { subtractKg } from './kilogram';

const PCT_SCALE = 2;
const NUMERIC_PATTERN = /^-?\d+(\.\d+)?$/;
const PCT_FORMAT = new Intl.NumberFormat('tr-TR', {
  minimumFractionDigits: 0,
  maximumFractionDigits: PCT_SCALE,
});

/** Rapor 9.1: fark yüzdesi çıkış net kilosuna göre hesaplanır; sonuç "-0.90" gibi dizedir. */
export function calculateDifferencePct(dispatchNetKg: Kilogram, deliveryNetKg: Kilogram): string {
  const dispatch = new Big(dispatchNetKg);
  if (dispatch.eq(0)) {
    throw new RangeError('Dispatch net kilogram is zero');
  }
  const pct = subtractKg(deliveryNetKg, dispatchNetKg).div(dispatch).times(100).toFixed(PCT_SCALE);
  return isNegativeZero(pct) ? pct.slice(1) : pct;
}

/** Yuvarlanınca sıfır olan küçük eksi fark "-0.00" çıkar; gösterimde yanlış eksi işareti olmasın. */
function isNegativeZero(pct: string): boolean {
  return pct.startsWith('-') && new Big(pct).eq(0);
}

function toNumericString(value: string): `${number}` {
  if (!isNumericString(value)) {
    throw new RangeError(`Invalid percentage value=${value}`);
  }
  return value;
}

function isNumericString(value: string): value is `${number}` {
  return NUMERIC_PATTERN.test(value);
}

/** "-0.90" → "-%0,9" (Türkçede yüzde işareti başa yazılır, eksi işareti ondan önce gelir). */
export function formatPct(pct: string): string {
  const formatted = PCT_FORMAT.format(toNumericString(pct));
  if (formatted === '-0') return '%0';
  return formatted.startsWith('-') ? `-%${formatted.slice(1)}` : `%${formatted}`;
}

const RATE_PATTERN = /^\d+(\.\d+)?$/;
const RATE_SCALE = 4;

/** Oran dizesi ("0.0400") yüzde metnine ("4"); fire oranı gösterimi için. Geçersizse undefined. */
export function rateToPct(rate: string): string | undefined {
  return RATE_PATTERN.test(rate) ? new Big(rate).times(100).toString() : undefined;
}

/** Türkçe yüzde girdisi ("4,5") API oranına ("0.0450"); geçersiz ya da `maxPct` üstüyse undefined. */
export function pctInputToRate(input: string, maxPct: number): string | undefined {
  const text = input.trim().replace(',', '.');
  if (!RATE_PATTERN.test(text)) return undefined;
  const pct = new Big(text);
  return pct.gt(maxPct) ? undefined : pct.div(100).toFixed(RATE_SCALE);
}
