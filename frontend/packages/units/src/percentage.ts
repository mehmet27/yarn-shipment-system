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
  return subtractKg(deliveryNetKg, dispatchNetKg).div(dispatch).times(100).toFixed(PCT_SCALE);
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

/** "-0.90" → "%-0,9" (Türkçede yüzde işareti başa yazılır). */
export function formatPct(pct: string): string {
  return `%${PCT_FORMAT.format(toNumericString(pct))}`;
}
