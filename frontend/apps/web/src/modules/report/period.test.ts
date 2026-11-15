import { describe, expect, it } from 'vitest';
import { toPeriodParams } from './period';

describe('toPeriodParams', () => {
  it('bugün dahil son N günü verir', () => {
    expect(toPeriodParams('7', new Date('2026-11-03T10:00:00Z'))).toEqual({
      from: '2026-10-28',
      to: '2026-11-03',
    });
    expect(toPeriodParams('30', new Date('2026-11-03T10:00:00Z')).from).toBe('2026-10-05');
  });

  it('gün İstanbul saatine göre hesaplanır: UTC gece yarısından önceki yerel gece bir sonraki gündür', () => {
    // 22:30 UTC = 01:30 İstanbul (ertesi gün)
    expect(toPeriodParams('7', new Date('2026-11-14T22:30:00Z'))).toEqual({
      from: '2026-11-09',
      to: '2026-11-15',
    });
  });
});
