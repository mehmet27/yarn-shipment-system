import { describe, expect, it } from 'vitest';
import { lotState, lotSuggestions } from './lotChoice';

const lots = [
  { lotNo: 'P-1', productId: 'a', yarnSupplierId: 's' },
  { lotNo: 'P-2', productId: 'a' },
  { lotNo: 'P-3', productId: 'b', yarnSupplierId: 's' },
  { lotNo: 'P-4', productId: 'a', yarnSupplierId: 't' },
];
const owner = { productId: 'a', supplierId: 's' };

describe('parti seçimi', () => {
  it('yalnızca seçili iplik ve tedarikçinin partilerini önerir', () => {
    expect(lotSuggestions(lots, owner)).toEqual(['P-1', 'P-2']);
    expect(lotSuggestions(lots, { productId: 'a', supplierId: undefined })).toEqual([]);
  });

  it('yazılan numaranın durumunu söyler', () => {
    expect(lotState(' ', lots, owner)).toBe('none');
    expect(lotState('p-1', lots, owner)).toBe('existing');
    expect(lotState('P-3', lots, owner)).toBe('conflict');
    expect(lotState('P-4', lots, owner)).toBe('conflict');
    expect(lotState('P-9', lots, owner)).toBe('new');
    expect(lotState('P-1', undefined, owner)).toBe('unknown');
  });
});
