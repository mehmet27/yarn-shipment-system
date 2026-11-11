import { describe, expect, it } from 'vitest';
import type { FabricDraft } from './settlementDraft';
import {
  EMPTY_FABRIC_DRAFT,
  EMPTY_RETURN_DRAFT,
  toFabricRequest,
  toInvoiceRequest,
  toRateRequest,
  toReturnRequest,
} from './settlementDraft';

const fabric: FabricDraft = {
  ...EMPTY_FABRIC_DRAFT,
  fabricType: 'Süprem',
  netKg: '32.660',
  waybillNo: 'IRS-1',
};

describe('toFabricRequest', () => {
  it('iplik satırı yoksa yalnız kumaş bilgisini gönderir', () => {
    expect(toFabricRequest({ ...fabric, rollCount: '12' })).toEqual({
      fabricType: 'Süprem',
      netKg: '32660.000',
      waybillNo: 'IRS-1',
      rollCount: 12,
    });
  });

  it('eksik alan, geçersiz kilo ve top sayısını reddeder', () => {
    expect(toFabricRequest(EMPTY_FABRIC_DRAFT)).toBe('required');
    expect(toFabricRequest({ ...fabric, netKg: 'abc' })).toBe('invalidKg');
    expect(toFabricRequest({ ...fabric, rollCount: '0' })).toBe('invalidRollCount');
  });

  it('iplik dağılımı toplamı kumaş kilosuna eşit olmalı', () => {
    const rows = [
      { yarnType: 'A', netKg: '20.000' },
      { yarnType: 'B', netKg: '12.660' },
    ];
    expect(toFabricRequest({ ...fabric, yarns: rows })).toMatchObject({
      yarns: [
        { yarnType: 'A', netKg: '20000.000' },
        { yarnType: 'B', netKg: '12660.000' },
      ],
    });
    expect(toFabricRequest({ ...fabric, netKg: '32,660', yarns: rows })).toBe(
      'distributionMismatch',
    );
    expect(toFabricRequest({ ...fabric, yarns: [{ yarnType: '', netKg: '1' }] })).toBe('required');
    expect(toFabricRequest({ ...fabric, yarns: [{ yarnType: 'A', netKg: 'x' }] })).toBe(
      'invalidKg',
    );
  });
});

describe('toReturnRequest', () => {
  it('isteğe bağlı alanları boşsa göndermez', () => {
    expect(toReturnRequest({ ...EMPTY_RETURN_DRAFT, yarnType: '30/1', netKg: '100' })).toEqual({
      direction: 'RETURN',
      yarnType: '30/1',
      netKg: '100.000',
    });
    expect(
      toReturnRequest({
        direction: 'FROM_SUBCONTRACTOR',
        yarnType: '30/1',
        netKg: '5,5',
        lotId: ' L1 ',
        waybillNo: 'W1',
      }),
    ).toMatchObject({ lotId: 'L1', waybillNo: 'W1', netKg: '5.500' });
  });

  it('eksik cins ve geçersiz kiloyu reddeder', () => {
    expect(toReturnRequest(EMPTY_RETURN_DRAFT)).toBe('required');
    expect(toReturnRequest({ ...EMPTY_RETURN_DRAFT, yarnType: 'A', netKg: '' })).toBe('invalidKg');
  });
});

describe('toRateRequest', () => {
  it('yüzdeyi orana çevirir; sınır ve gerekçe zorunlu', () => {
    expect(toRateRequest('4,5', 'sözleşme')).toEqual({ fireRate: '0.0450', reason: 'sözleşme' });
    expect(toRateRequest('21', 'x')).toBe('invalidRate');
    expect(toRateRequest('4', ' ')).toBe('required');
  });
});

describe('toInvoiceRequest', () => {
  it('fatura no ve tarih zorunlu, not isteğe bağlı', () => {
    expect(toInvoiceRequest('F-1', '2026-11-10', '')).toEqual({
      invoiceNo: 'F-1',
      invoicedOn: '2026-11-10',
    });
    expect(toInvoiceRequest('F-1', '2026-11-10', ' not ')).toMatchObject({ note: 'not' });
    expect(toInvoiceRequest('', '2026-11-10', '')).toBe('required');
    expect(toInvoiceRequest('F-1', '10.11.2026', '')).toBe('required');
  });
});
