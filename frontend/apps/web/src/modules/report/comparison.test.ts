import { describe, expect, it } from 'vitest';
import { previousPeriod, toChange } from './comparison';

describe('previousPeriod', () => {
  it('aynı uzunlukta, hemen önceki dönemi verir', () => {
    expect(previousPeriod({ from: '2026-11-01', to: '2026-11-30' })).toEqual({
      from: '2026-10-02',
      to: '2026-10-31',
    });
    expect(previousPeriod({ from: '2026-03-01', to: '2026-03-01' })).toEqual({
      from: '2026-02-28',
      to: '2026-02-28',
    });
  });
});

describe('toChange', () => {
  it('artışı ve azalışı tam yüzde olarak verir', () => {
    expect(toChange(125, 100)).toEqual({ direction: 'up', percent: 25 });
    expect(toChange(50, 200)).toEqual({ direction: 'down', percent: 75 });
  });

  it('eşitse ya da fark yüzde yarımın altındaysa "aynı" der', () => {
    expect(toChange(0, 0)).toEqual({ direction: 'same', percent: 0 });
    expect(toChange(1001, 1000)).toEqual({ direction: 'same', percent: 0 });
  });

  it('önceki dönem sıfırsa ya da sayı okunamıyorsa oran vermez', () => {
    expect(toChange(12, 0)).toBeNull();
    expect(toChange(Number.NaN, 5)).toBeNull();
  });
});
