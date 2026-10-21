import { describe, expect, it } from 'vitest';
import type { DispatchDraft } from './dispatchDraft';
import { EMPTY_DISPATCH_DRAFT, toDispatchRequest, validateDispatch } from './dispatchDraft';

const VALID: DispatchDraft = {
  grossKg: '2.000',
  tareKg: '17,5',
  bagCount: '40',
  scaleTicketNo: ' KF-1 ',
  sealNo: ' M-77 ',
};

describe('dispatchDraft', () => {
  it('geçerli taslağı API biçiminde isteğe çevirir', () => {
    expect(toDispatchRequest(VALID)).toEqual({
      grossKg: '2000.000',
      tareKg: '17.500',
      bagCount: 40,
      sealNo: 'M-77',
      scaleTicketNo: 'KF-1',
    });
  });

  it('boş kantar fişini isteğe koymaz', () => {
    expect(toDispatchRequest({ ...VALID, scaleTicketNo: ' ' })).not.toHaveProperty('scaleTicketNo');
  });

  it('hataları sırayla bildirir', () => {
    expect(validateDispatch(EMPTY_DISPATCH_DRAFT)).toBe('invalidKg');
    expect(validateDispatch({ ...VALID, tareKg: '3.000' })).toBe('tareExceedsGross');
    expect(validateDispatch({ ...VALID, bagCount: '0' })).toBe('invalidBagCount');
    expect(validateDispatch({ ...VALID, sealNo: ' ' })).toBe('sealNoRequired');
    expect(validateDispatch(VALID)).toBeUndefined();
    expect(toDispatchRequest({ ...VALID, sealNo: '' })).toBeUndefined();
  });
});
