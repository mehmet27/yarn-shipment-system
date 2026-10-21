import { describe, expect, it } from 'vitest';
import type { Kilogram } from './kilogram';
import { calculateDifferencePct, formatPct } from './percentage';

const kg = (value: string) => value as Kilogram;

describe('calculateDifferencePct', () => {
  it('teslim eksikse farkı negatif yüzde olarak verir', () => {
    expect(calculateDifferencePct(kg('2000.000'), kg('1982.000'))).toBe('-0.90');
  });

  it('çıkış net kilosu sıfırsa hata verir', () => {
    expect(() => calculateDifferencePct(kg('0.000'), kg('1.000'))).toThrow(RangeError);
  });
});

describe('formatPct', () => {
  it('Türkçe yüzde biçimiyle gösterir', () => {
    expect(formatPct('-0.90')).toBe('%-0,9');
  });

  it('sayı olmayan değerde hata verir', () => {
    expect(() => formatPct('abc')).toThrow(RangeError);
  });
});
