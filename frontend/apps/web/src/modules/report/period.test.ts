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
});
