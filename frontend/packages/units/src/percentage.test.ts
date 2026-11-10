import { describe, expect, it } from 'vitest';
import type { Kilogram } from './kilogram';
import {
  calculateDifferencePct,
  fireRateToPctInput,
  formatPct,
  parseFireRatePct,
} from './percentage';

const kg = (value: string) => value as Kilogram;

describe('calculateDifferencePct', () => {
  it('teslim eksikse farkı negatif yüzde olarak verir', () => {
    expect(calculateDifferencePct(kg('2000.000'), kg('1982.000'))).toBe('-0.90');
  });

  it('yuvarlanınca sıfır olan küçük farkta eksi işareti taşımaz', () => {
    expect(calculateDifferencePct(kg('2000.000'), kg('1999.999'))).toBe('0.00');
  });

  it('çıkış net kilosu sıfırsa hata verir', () => {
    expect(() => calculateDifferencePct(kg('0.000'), kg('1.000'))).toThrow(RangeError);
  });
});

describe('formatPct', () => {
  it('Türkçe yüzde biçimiyle gösterir', () => {
    expect(formatPct('-0.90')).toBe('-%0,9');
  });

  it('eksi sıfırı "%0" olarak gösterir', () => {
    expect(formatPct('-0.00')).toBe('%0');
    expect(formatPct('-0.004')).toBe('%0');
  });

  it('sayı olmayan değerde hata verir', () => {
    expect(() => formatPct('abc')).toThrow(RangeError);
  });
});

describe('fire oranı', () => {
  it('yüzdeyi kesire çevirir', () => {
    expect(parseFireRatePct('4')).toBe(0.04);
    expect(parseFireRatePct('4,25')).toBe(0.0425);
    expect(parseFireRatePct('20')).toBe(0.2);
    expect(parseFireRatePct('0')).toBe(0);
  });

  it.each(['', '20,01', '21', 'x', '1,234', '-1'])('"%s" geçersizdir', (input) => {
    expect(parseFireRatePct(input)).toBeUndefined();
  });

  it('kesri yüzde girdisine çevirir', () => {
    expect(fireRateToPctInput(0.04)).toBe('4');
    expect(fireRateToPctInput(0.0425)).toBe('4,25');
  });
});
