import { describe, expect, it } from 'vitest';
import { sumFabricKg, sumInTransitKg } from './stockTotals';

describe('stockTotals', () => {
  it('veri gelmediyse toplam yoktur', () => {
    expect(sumInTransitKg(undefined)).toBeUndefined();
    expect(sumFabricKg(undefined)).toBeUndefined();
  });

  it('yoldaki iplik ve gelen kumaş kilolarını toplar', () => {
    const balances = {
      totalKg: '10.000',
      items: [
        {
          subcontractorId: 'a',
          subcontractorTitle: 'A',
          yarnType: 'P',
          balanceKg: '5.000',
          inTransitKg: '1.500',
        },
        {
          subcontractorId: 'b',
          subcontractorTitle: 'B',
          yarnType: 'P',
          balanceKg: '5.000',
          inTransitKg: '2.250',
        },
      ],
    };
    expect(sumInTransitKg(balances)).toBe('3.750');
    const row = {
      subcontractorId: 'a',
      subcontractorName: 'A',
      sentKg: '0.000',
      returnedYarnKg: '0.000',
      actualFireRate: '0',
      shortageKg: '0.000',
      overLimitShipmentCount: 0,
      overLimitFireKg: '0.000',
    };
    const rows = [
      { ...row, fabricKg: '100.000' },
      { ...row, fabricKg: '20.500' },
    ];
    expect(sumFabricKg(rows)).toBe('120.500');
  });
});
