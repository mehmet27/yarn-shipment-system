import { describe, expect, it } from 'vitest';
import { monthName, monthPeriod, recentMonths, toInvoicePeriod } from './monthClose';

describe('ay sonu kapanışı dönemleri', () => {
  it('bu aydan geriye ayları sıralar, yıl sınırını geçer', () => {
    expect(recentMonths(new Date('2026-02-10T09:00:00Z'), 4)).toEqual([
      '2026-02',
      '2026-01',
      '2025-12',
      '2025-11',
    ]);
  });

  it('ay İstanbul saatine göredir: ayın son gecesi UTC 21:00 sonrası yeni aydır', () => {
    expect(recentMonths(new Date('2026-10-31T22:30:00Z'), 1)).toEqual(['2026-11']);
  });

  it('ayın ilk ve son gününü verir (şubat ve 31 çeken ay)', () => {
    expect(monthPeriod('2026-02')).toEqual({ from: '2026-02-01', to: '2026-02-28' });
    expect(monthPeriod('2026-12')).toEqual({ from: '2026-12-01', to: '2026-12-31' });
  });

  it('faturalanacaklar için ay sonundan geriye bir yıl bakar', () => {
    expect(toInvoicePeriod('2026-10')).toEqual({ from: '2025-10-31', to: '2026-10-31' });
  });

  it('ay adını Türkçe yazar', () => {
    expect(monthName('2026-10')).toBe('Ekim 2026');
  });
});
