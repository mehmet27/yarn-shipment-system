import { describe, expect, it } from 'vitest';
import type { Kilogram } from './kilogram';
import { calculateNetKg, formatKg, isKilogram, parseKg } from './kilogram';

const kg = (value: string) => value as Kilogram;

describe('parseKg', () => {
  it.each([
    ['1.982,5', '1982.500'],
    ['1982,5', '1982.500'],
    ['  12 ', '12.000'],
    ['0,125', '0.125'],
  ])('Türkçe girişi "%s" API biçimine "%s" çevirir', (input, expected) => {
    expect(parseKg(input)).toBe(expected);
  });

  it.each(['', 'abc', '1,2345', '1982.5', '-5', '19.82'])(
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
