import { describe, expect, it } from 'vitest';
import { formatDifference } from './formatDifference';

describe('formatDifference', () => {
  it('eksi farkı "-3 kg" yazar; "-3.000" üç bin okunmasın', () => {
    expect(formatDifference('-3.000', '-0.50')).toBe('-3 kg · -%0,5');
  });

  it('artı farkı "+3 kg" yazar', () => {
    expect(formatDifference('3.000', undefined)).toBe('+3 kg');
  });

  it('bin kiloluk farkı binlik ayraçla yazar', () => {
    expect(formatDifference('-3000.000', undefined)).toBe('-3.000 kg');
  });
});
