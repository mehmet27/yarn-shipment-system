import { describe, expect, it } from 'vitest';
import type { DeliveryDraft } from './deliveryDraft';
import { addBagKey, EMPTY_DRAFT, toDeliveryRequest, validateStep } from './deliveryDraft';

const VALID: DeliveryDraft = {
  ...EMPTY_DRAFT,
  sealCondition: 'INTACT',
  sealNo: ' M-1 ',
  bagKeys: ['B1', 'B2'],
  grossKg: '1.995,25',
  tareKg: '17',
  bagCount: '2',
  ticket: { kind: 'READ' },
};

describe('deliveryDraft', () => {
  it('tekrar okutulan ve boş anahtarı eklemez', () => {
    expect(addBagKey(addBagKey(['B1'], ' B1 '), '  ')).toEqual(['B1']);
  });

  it('mühür yoksa numara istemez, varsa ister', () => {
    expect(validateStep('seal', { ...EMPTY_DRAFT, sealCondition: 'MISSING' })).toBeUndefined();
    expect(validateStep('seal', { ...EMPTY_DRAFT, sealCondition: 'DAMAGED' })).toBe(
      'sealNoRequired',
    );
    expect(validateStep('seal', EMPTY_DRAFT)).toBe('sealConditionRequired');
  });

  it('dara brütten büyükse ve kilo biçimi hatalıysa reddeder', () => {
    expect(validateStep('weighing', { ...VALID, tareKg: '3000' })).toBe('tareExceedsGross');
    expect(validateStep('weighing', { ...VALID, grossKg: '1995.25.1' })).toBe('invalidKg');
    expect(validateStep('weighing', { ...VALID, bagCount: '0' })).toBe('invalidBagCount');
    expect(validateStep('weighing', { ...VALID, ticket: undefined })).toBe('ticketRequired');
  });

  it('isteği API biçiminde kurar, boş irsaliyeyi göndermez', () => {
    expect(toDeliveryRequest(VALID)).toEqual({
      grossKg: '1995.250',
      tareKg: '17.000',
      bagCount: 2,
      sealCondition: 'INTACT',
      sealNo: 'M-1',
      bagUniqueKeys: ['B1', 'B2'],
      scanMethod: 'SCAN',
    });
  });

  it('irsaliye alanlarını ekler; mühür yoksa sealNo göndermez', () => {
    const request = toDeliveryRequest({
      ...VALID,
      sealCondition: 'MISSING',
      document: { shipmentNo: 'SVK-1', date: '', netKg: '1.978,25', bagCount: '' },
    });
    expect(request).not.toHaveProperty('sealNo');
    expect(request?.document).toEqual({ shipmentNo: 'SVK-1', netKg: '1978.250' });
  });
});
