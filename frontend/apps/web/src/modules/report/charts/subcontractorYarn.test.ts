import { sumSignedKg } from '@iplik/units';
import { describe, expect, it } from 'vitest';
import { sumBySubcontractor } from './subcontractorYarn';

const row = (subcontractorId: string, yarnType: string, kg: readonly [string, string]) => ({
  subcontractorId,
  subcontractorTitle: `Fason ${subcontractorId}`,
  yarnType,
  balanceKg: kg[0],
  inTransitKg: kg[1],
});

describe('sumBySubcontractor', () => {
  const items = [
    row('a', 'Penye', ['1982.500', '500.000']),
    row('b', 'Penye', ['1000.000', '0.000']),
    row('a', 'Süprem', ['750.250', '0.000']),
  ];

  it('iplik cinslerini fasoncu başına toplar', () => {
    const result = sumBySubcontractor({ items, totalKg: '3732.750' });
    expect(result).toEqual([
      {
        subcontractorId: 'a',
        subcontractorTitle: 'Fason a',
        balanceKg: '2732.750',
        inTransitKg: '500.000',
      },
      {
        subcontractorId: 'b',
        subcontractorTitle: 'Fason b',
        balanceKg: '1000.000',
        inTransitKg: '0.000',
      },
    ]);
  });

  it('eksi bakiyeli iplik cinsini de toplama katar', () => {
    const [first] = sumBySubcontractor({
      items: [row('a', 'Penye', ['100.000', '0.000']), row('a', 'Viskon', ['-40.500', '0.000'])],
      totalKg: '59.500',
    });
    expect(first?.balanceKg).toBe('59.500');
  });

  it('fasoncu toplamlarının toplamı bakiye ekranının genel toplamına eşittir', () => {
    const result = sumBySubcontractor({ items, totalKg: '3732.750' });
    expect(Number(sumSignedKg(result.map((entry) => entry.balanceKg)))).toBe(Number('3732.750'));
  });
});
