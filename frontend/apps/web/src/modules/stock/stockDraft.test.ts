import { describe, expect, it } from 'vitest';
import { emptyProductionDraft, splitKg, toProductionRequest } from './stockDraft';

const yarn = (value: string, ratio: string) => ({ product: { value, label: value }, ratio });
const draft = (...yarns: ReturnType<typeof yarn>[]) => ({
  ...emptyProductionDraft('4'),
  fabricType: 'Süprem',
  netKg: '100',
  yarns,
});

describe('merkez kumaş girişi taslağı', () => {
  it('toplam kiloyu oranlara böler; satırların toplamı yazılan kiloya eşittir', () => {
    expect(splitKg('100.000', [9500, 500])).toEqual(['95.000', '5.000']);
    expect(splitKg('0.100', [3333, 3333, 3334])).toEqual(['0.033', '0.033', '0.034']);
  });

  it('isteğe iplik başına kumaş kilosu yazar', () => {
    expect(toProductionRequest(draft(yarn('a', '95'), yarn('b', '5')))).toMatchObject({
      yarns: [
        { productId: 'a', netKg: '95.000' },
        { productId: 'b', netKg: '5.000' },
      ],
    });
  });

  it('toplamı 100 olmayan, tekrarlanan ya da okunamayan oranı göndermez', () => {
    expect(toProductionRequest(draft(yarn('a', '90')))).toBe('ratioTotal');
    expect(toProductionRequest(draft(yarn('a', '50'), yarn('a', '50')))).toBe('duplicateYarn');
    expect(toProductionRequest(draft(yarn('a', 'abc')))).toBe('invalidRatio');
    expect(toProductionRequest(draft(yarn('a', '')))).toBe('required');
  });
});
