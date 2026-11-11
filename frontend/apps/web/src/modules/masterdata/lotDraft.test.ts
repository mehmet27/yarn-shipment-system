import { describe, expect, it } from 'vitest';
import { localToday, validateLot } from './lotDraft';

const VALID = {
  siteId: 'site-merkez',
  lotNo: 'p-2026-1',
  productId: 'prd-0',
  qualityGradeId: 'grd-a',
  yarnSupplierId: '',
  productionDate: '2026-10-10',
};

describe('validateLot', () => {
  it('eksiksiz partiyi kabul eder', () => {
    expect(validateLot(VALID, '2026-10-30')).toEqual({});
  });

  it('boş alanları, geçersiz numarayı ve ileri tarihi işaretler', () => {
    expect(validateLot({ ...VALID, productId: '', lotNo: 'P 1' }, '2026-10-30')).toEqual({
      productId: 'required',
      lotNo: 'invalidLotNo',
    });
    expect(validateLot(VALID, '2026-10-09')).toEqual({ productionDate: 'futureDate' });
  });

  it('yerel günü YYYY-MM-DD verir', () => {
    expect(localToday(new Date(2026, 0, 5))).toBe('2026-01-05');
  });
});
