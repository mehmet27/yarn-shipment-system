import { describe, expect, it } from 'vitest';
import type { Kilogram } from './kilogram';
import {
  calculateNetKg,
  formatKg,
  isZeroKg,
  formatKgText,
  formatSignedKg,
  isKilogram,
  parseKg,
  sumKg,
} from './kilogram';

const kg = (value: string) => value as Kilogram;

describe('parseKg', () => {
  it.each([
    ['1.982,5', '1982.500'],
    ['1982,5', '1982.500'],
    ['  12 ', '12.000'],
    ['0,125', '0.125'],
    ['100.000', '100000.000'],
  ])('Türkçe girişi "%s" API biçimine "%s" çevirir', (input, expected) => {
    expect(parseKg(input)).toBe(expected);
  });

  it.each(['', 'abc', '1,2345', '1982.5', '-5', '19.82', '100.001', '99.999.999'])(
    'geçersiz girişte "%s" boş döner',
    (input) => {
      expect(parseKg(input)).toBeUndefined();
    },
  );
});

describe('calculateNetKg', () => {
  it('brütten darayı ondalık hatası olmadan çıkarır', () => {
    expect(calculateNetKg(kg('0.3'), kg('0.1'))).toBe('0.200');
    expect(calculateNetKg(kg('2000.000'), kg('17.500'))).toBe('1982.500');
  });

  it('dara bruttan büyükse net kiloyu boş bırakır', () => {
    expect(calculateNetKg(kg('10'), kg('10.001'))).toBeUndefined();
  });
});

describe('formatKg', () => {
  it('Türkçe binlik ve ondalık ayırıcıyla gösterir', () => {
    expect(formatKg(kg('1982.500'))).toBe('1.982,5 kg');
  });
});

describe('isKilogram', () => {
  it('yalnızca API biçimini kabul eder', () => {
    expect(isKilogram('1982.500')).toBe(true);
    expect(isKilogram('1982,5')).toBe(false);
  });
});

describe('sumKg ve formatSignedKg', () => {
  it('kiloları toplar, boş listede 0 verir', () => {
    expect(sumKg([kg('19488.000'), kg('13492.000'), kg('2036.000')])).toBe('35016.000');
    expect(sumKg([])).toBe('0.000');
    expect(formatKgText('1982.500')).toBe('1.982,5 kg');
    expect(formatKgText('x')).toBe('x');
    expect(formatKgText('-5058.347')).toBe('−5.058,347 kg');
  });

  it('işaretli farkı Türkçe biçimler', () => {
    expect(formatSignedKg('-12.500')).toBe('-12,5 kg');
    expect(formatSignedKg('7.000')).toBe('+7 kg');
    expect(formatSignedKg('x')).toBe('x');
  });
});

describe('isZeroKg', () => {
  it('sıfırı tanır, sıfır olmayanı ve sayı olmayanı tanımaz', () => {
    expect(isZeroKg('0.000')).toBe(true);
    expect(isZeroKg('0')).toBe(true);
    expect(isZeroKg('12.500')).toBe(false);
    expect(isZeroKg('-3.000')).toBe(false);
    expect(isZeroKg('abc')).toBe(false);
  });
});
