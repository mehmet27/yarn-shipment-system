import { describe, expect, it } from 'vitest';
import type { DispatchDraft } from './dispatchDraft';
import { EMPTY_DISPATCH_DRAFT, toDispatchRequest, validateDispatch } from './dispatchDraft';

const VALID: DispatchDraft = {
  grossKg: '2.000',
  tareKg: '17,5',
  bagCount: '40',
  shortfallReason: '',
  ticket: { kind: 'PHOTO' },
};

describe('dispatchDraft', () => {
  it('fiş sonucu yoksa devam ettirmez', () => {
    expect(validateDispatch({ ...VALID, ticket: undefined })).toBe('ticketRequired');
  });

  it('fotoğraf seçilmiş ama gönderilmemişse ayrı hata verir ve isteği kurmaz', () => {
    expect(validateDispatch({ ...VALID, ticket: undefined }, true)).toBe('ticketNotSent');
    expect(toDispatchRequest(VALID, true)).toBeUndefined();
  });

  it('geçerli taslağı API biçiminde isteğe çevirir', () => {
    expect(toDispatchRequest(VALID)).toEqual({
      grossKg: '2000.000',
      tareKg: '17.500',
      bagCount: 40,
    });
  });

  it('eksik gönderim nedeni doluysa isteğe girer, boşsa girmez', () => {
    expect(toDispatchRequest({ ...VALID, shortfallReason: ' Stok yetmedi ' })).toMatchObject({
      shortfallReason: 'Stok yetmedi',
    });
    expect(toDispatchRequest(VALID)).not.toHaveProperty('shortfallReason');
  });

  it('hataları sırayla bildirir', () => {
    expect(validateDispatch(EMPTY_DISPATCH_DRAFT)).toBe('invalidKg');
    expect(validateDispatch({ ...VALID, tareKg: '3.000' })).toBe('tareExceedsGross');
    expect(validateDispatch({ ...VALID, bagCount: '0' })).toBe('invalidBagCount');
    expect(validateDispatch(VALID)).toBeUndefined();
    expect(toDispatchRequest({ ...VALID, ticket: undefined })).toBeUndefined();
  });
});
