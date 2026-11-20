import { sumKg } from '@iplik/units';
import { describe, expect, it } from 'vitest';
import { foldOthers, MAX_BARS, sortByKgDesc, toKgBars } from './barItems';

const entry = (label: string, kg: string) => ({ key: label, label, kg });
const other = (count: number) => `Diğer ${String(count)}`;

describe('barItems', () => {
  it('kiloya göre büyükten küçüğe, eşitlikte Türkçe ada göre sıralar', () => {
    const sorted = sortByKgDesc([
      entry('Çınar', '10.000'),
      entry('Aksu', '10.000'),
      entry('Zed', '50.500'),
    ]);
    expect(sorted.map((row) => row.label)).toEqual(['Zed', 'Aksu', 'Çınar']);
  });

  it('satır sayısı sınırı aşmıyorsa hiçbir satırı birleştirmez', () => {
    const entries = Array.from({ length: MAX_BARS + 1 }, (_, index) =>
      entry(`F${String(index)}`, '1.000'),
    );
    expect(foldOthers(entries, other)).toHaveLength(MAX_BARS + 1);
  });

  it('fazla satırları "Diğer" satırında toplar ve genel toplamı değiştirmez', () => {
    const entries = Array.from({ length: MAX_BARS + 4 }, (_, index) =>
      entry(`F${String(index)}`, `${String(index + 1)}.250`),
    );
    const folded = foldOthers(entries, other);
    expect(folded).toHaveLength(MAX_BARS + 1);
    expect(folded.at(-1)?.label).toBe('Diğer 4');
    expect(sumKg(folded.map((row) => row.kg))).toBe(sumKg(entries.map((row) => row.kg)));
  });

  it('eksi kiloyu çubuksuz ve dikkat rengiyle, değeri yazıyla verir', () => {
    const [bar] = toKgBars([entry('Viskon', '-12.500')], (kg) => `${kg} kg`);
    expect(bar).toMatchObject({ value: -12.5, valueLabel: '-12.500 kg', tone: 'danger' });
  });
});
