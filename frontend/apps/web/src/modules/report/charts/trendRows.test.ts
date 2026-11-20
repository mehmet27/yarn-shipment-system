import type { MonthlyFlowView } from '@iplik/api-client';
import { describe, expect, it } from 'vitest';
import { formatMonth, isTrendEmpty, toTrendRows } from './trendRows';

const flow = (
  month: string,
  kg: readonly [string, string],
  shipmentCount: number,
): MonthlyFlowView => ({
  month,
  from: `${month}-01`,
  to: `${month}-28`,
  shipmentCount,
  sentKg: kg[0],
  returnedYarnKg: '0.000',
  fabricKg: kg[1],
  actualFireRate: '0.0400',
  shortageKg: '0.000',
});

describe('trendRows', () => {
  it('ayı Türkçe kısa adıyla yazar', () => {
    expect(formatMonth('2026-11')).toBe('Kas 26');
    expect(formatMonth('2026-01')).toBe('Oca 26');
  });

  it('grafik satırı sunucunun kilolarını değiştirmeden taşır', () => {
    expect(toTrendRows([flow('2026-11', ['34032.000', '32660.500'], 4)])).toEqual([
      { month: 'Kas 26', sentKg: 34032, fabricKg: 32660.5 },
    ]);
  });

  it('hiçbir ayda sevkiyat yoksa eğilim boş sayılır', () => {
    expect(isTrendEmpty([flow('2026-10', ['0.000', '0.000'], 0)])).toBe(true);
    expect(
      isTrendEmpty([
        flow('2026-10', ['0.000', '0.000'], 0),
        flow('2026-11', ['10.000', '9.000'], 1),
      ]),
    ).toBe(false);
  });
});
