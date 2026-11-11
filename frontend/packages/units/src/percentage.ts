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

const FIRE_RATE_INPUT = /^\d{1,3}([.,]\d{1,2})?$/;
const FIRE_RATE_MAX_PCT = 20;
const PCT_DIVISOR = 100;
const FIRE_RATE_SCALE = 4;

/** Yüzde girdisini ("4", "4,5") API kesir dizesine çevirir ("0.0450"); 0–20 dışı ya da 2 ondalıktan fazlası geçersizdir. */
export function parseFireRatePct(input: string): string | undefined {
  const trimmed = input.trim();
  if (!FIRE_RATE_INPUT.test(trimmed)) return undefined;
  const pct = new Big(trimmed.replace(',', '.'));
  return pct.gt(FIRE_RATE_MAX_PCT) ? undefined : pct.div(PCT_DIVISOR).toFixed(FIRE_RATE_SCALE);
}

/** Kesri yüzde girdisine çevirir (0.04 → "4"); alanda gösterilecek varsayılan için. */
export function fireRateToPctInput(rate: string): string {
  return new Big(rate).times(PCT_DIVISOR).round(2).toString().replace('.', ',');
}
