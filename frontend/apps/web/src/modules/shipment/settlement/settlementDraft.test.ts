import type { RecipeLineView } from '@iplik/api-client';
import type { Kilogram } from '@iplik/units';
import { describe, expect, it } from 'vitest';
import { remainingFabricKg, yarnOptionsOf } from './fabricLimits';
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

describe('toFabricRequest: kalan kilo', () => {
  it('kumaş kilosu kalan kiloyu aşarsa göndermez, eşitse gönderir', () => {
    expect(toFabricRequest({ ...fabric, netKg: '1.900' }, '1800.000' as Kilogram)).toBe(
      'exceedsRemaining',
    );
    expect(toFabricRequest({ ...fabric, netKg: '1.900' }, '1900.000' as Kilogram)).toMatchObject({
      netKg: '1900.000',
    });
  });
});

describe('remainingFabricKg', () => {
  it('gönderilenden gelen çıkar, 0 altına inmez', () => {
    expect(remainingFabricKg('2000.000', '1900.500')).toBe('99.500');
    expect(remainingFabricKg('2000.000', '2100.000')).toBe('0.000');
    expect(remainingFabricKg('abc', '1')).toBeUndefined();
    // faturalanan kilo kumaş gibi düşer: 200 − 25 − 67
    expect(remainingFabricKg('200.000', '25.000', '67.000')).toBe('108.000');
  });
});

describe('yarnOptionsOf', () => {
  it('değer sunucunun anahtarı, etiket ipliğin adıdır; tekrarları ve boşları atar', () => {
    const lines = [
      { lineNo: 1, yarnTypeId: 'prd-1', yarnTypeName: '30/1 Penye', netKg: '1.000' },
      { lineNo: 2, yarnTypeText: ' 20 denye likra ', netKg: '1.000' },
      { lineNo: 3, yarnTypeId: 'prd-1', yarnTypeName: '30/1 Penye', netKg: '1.000' },
      { lineNo: 4, netKg: '1.000' },
    ] as RecipeLineView[];
    expect(yarnOptionsOf(lines)).toEqual([
      { value: 'prd-1', label: '30/1 Penye' },
      { value: '20 denye likra', label: '20 denye likra' },
    ]);
  });
});

describe('toReturnRequest', () => {
  it('isteğe bağlı alanları boşsa göndermez', () => {
    expect(toReturnRequest({ ...EMPTY_RETURN_DRAFT, yarnType: '30/1', netKg: '100' })).toEqual({
      yarnType: '30/1',
      netKg: '100.000',
    });
    expect(
      toReturnRequest({
        yarnType: '30/1',
        netKg: '5,5',
        lot: { value: 'lot-1', label: 'L1' },
        waybillNo: 'W1',
      }),
    ).toMatchObject({ lotId: 'lot-1', waybillNo: 'W1', netKg: '5.500' });
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
  const draft = { invoiceNo: 'F-1', invoicedOn: '2026-11-10', note: '', lines: [] };

  it('fatura no ve tarih zorunlu, not isteğe bağlı', () => {
    expect(toInvoiceRequest(draft)).toEqual({ invoiceNo: 'F-1', invoicedOn: '2026-11-10' });
    expect(toInvoiceRequest({ ...draft, note: ' not ' })).toMatchObject({ note: 'not' });
    expect(toInvoiceRequest({ ...draft, invoiceNo: '' })).toBe('required');
    expect(toInvoiceRequest({ ...draft, invoicedOn: '10.11.2026' })).toBe('required');
  });

  it('dolu iplik satırları faturalanan kiloyu verir; boş satırlar atlanır', () => {
    const lines = [
      { yarnType: 'Pamuk', netKg: '20' },
      { yarnType: 'Viskon', netKg: '' },
    ];
    expect(toInvoiceRequest({ ...draft, lines }, '50')).toEqual({
      invoiceNo: 'F-1',
      invoicedOn: '2026-11-10',
      lines: [{ yarnType: 'Pamuk', netKg: '20.000' }],
    });
  });

  it('kilo yazılmamışsa, geçersizse ya da kalandan fazlaysa hata verir', () => {
    const empty = [{ yarnType: 'Pamuk', netKg: '' }];
    expect(toInvoiceRequest({ ...draft, lines: empty }, '50')).toBe('invoiceKgMissing');
    const tooMuch = [{ yarnType: 'Pamuk', netKg: '50,5' }];
    expect(toInvoiceRequest({ ...draft, lines: tooMuch }, '50')).toBe('invoiceExceeds');
    const bad = [{ yarnType: 'Pamuk', netKg: 'abc' }];
    expect(toInvoiceRequest({ ...draft, lines: bad }, '50')).toBe('invalidKg');
  });
});
