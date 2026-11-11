import { describe, expect, it } from 'vitest';
import { calculateFire, sumKg } from './fire';
import type { Kilogram } from './kilogram';

const kg = (value: string) => value as Kilogram;

describe('calculateFire', () => {
  it('Kırıkhan örneği: fire %4,03, hatalı 10,720 kg', () => {
    const result = calculateFire({
      sentKg: kg('34032.000'),
      returnedKg: kg('0.000'),
      claimKg: kg('0.000'),
      fabricKg: kg('32660.000'),
      fireRate: '0.0400',
    });
    expect(result).toEqual({
      baseKg: '34032.000',
      expectedFabricKg: '32670.720',
      shortageKg: '10.720',
      faultyKg: '10.720',
      actualFirePercent: '4.03',
    });
  });

  it('fazla kumaşta hatalı kilo sıfırdır', () => {
    const result = calculateFire({
      sentKg: kg('100.000'),
      returnedKg: kg('10.000'),
      claimKg: kg('0.000'),
      fabricKg: kg('90.000'),
      fireRate: '0.0400',
    });
    expect(result.faultyKg).toBe('0.000');
    expect(result.shortageKg).toBe('-3.600');
  });

  it('taban sıfırsa fire yüzdesi sıfırdır', () => {
    const zero = kg('0.000');
    const result = calculateFire({
      sentKg: zero,
      returnedKg: zero,
      claimKg: zero,
      fabricKg: zero,
      fireRate: '0.0400',
    });
    expect(result.actualFirePercent).toBe('0.00');
  });
});

describe('sumKg', () => {
  it('ondalıkları kayan nokta hatası olmadan toplar', () => {
    expect(sumKg(['0.1', '0.2'])).toBe('0.300');
    expect(sumKg([])).toBe('0.000');
  });
});
