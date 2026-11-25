import type { RecipeLineView } from '@iplik/api-client';
import { describe, expect, it } from 'vitest';
import { yarnOptionsOf } from './fabricLimits';
import type { FabricDraft } from './settlementDraft';
import {
  EMPTY_FABRIC_DRAFT,
  EMPTY_RETURN_DRAFT,
  toFabricRequest,
  toInvoiceRequest,
  toRateRequest,
  toReturnRequest,
} from './settlementDraft';

const penye = { yarnType: '30/1 Penye', lotId: 'lot-1', totalKg: '100.000', ownKg: '100.000' };
const likra = { yarnType: '20 denye likra', totalKg: '50.000', ownKg: '0.000' };
const options = [penye, likra];
const fabric: FabricDraft = {
  ...EMPTY_FABRIC_DRAFT,
  fabricType: 'Süprem',
  netKg: '32.660',
  waybillNo: 'IRS-1',
  ratios: { '30/1 Penye|lot-1': '60,5', '20 denye likra|': '39.5' },
};

describe('toFabricRequest', () => {
  it('oranlar toplamı 100 ise iplik, parti ve oranlarıyla gönderir', () => {
    expect(toFabricRequest({ ...fabric, rollCount: '12' }, options)).toEqual({
      fabricType: 'Süprem',
      netKg: '32660.000',
      waybillNo: 'IRS-1',
      rollCount: 12,
      yarns: [
        { yarnType: '30/1 Penye', lotId: 'lot-1', ratioPct: '60.5' },
        { yarnType: '20 denye likra', ratioPct: '39.5' },
      ],
    });
  });

  it('boş bırakılan iplik kullanılmamış sayılır', () => {
    expect(
      toFabricRequest({ ...fabric, ratios: { '30/1 Penye|lot-1': '100' } }, options),
    ).toMatchObject({ yarns: [{ yarnType: '30/1 Penye', lotId: 'lot-1', ratioPct: '100' }] });
  });

  it('eksik alan, geçersiz kilo ve top sayısını reddeder', () => {
    expect(toFabricRequest(EMPTY_FABRIC_DRAFT, options)).toBe('required');
    expect(toFabricRequest({ ...fabric, netKg: 'abc' }, options)).toBe('invalidKg');
    expect(toFabricRequest({ ...fabric, rollCount: '0' }, options)).toBe('invalidRollCount');
  });

  it('oran yoksa, bozuksa ya da toplam 100 değilse reddeder', () => {
    expect(toFabricRequest({ ...fabric, ratios: {} }, options)).toBe('ratioMissing');
    expect(toFabricRequest({ ...fabric, ratios: { '30/1 Penye|lot-1': 'x' } }, options)).toBe(
      'invalidRatio',
    );
    expect(toFabricRequest({ ...fabric, ratios: { '30/1 Penye|lot-1': '101' } }, options)).toBe(
      'invalidRatio',
    );
    expect(toFabricRequest({ ...fabric, ratios: { '30/1 Penye|lot-1': '99,99' } }, options)).toBe(
      'ratioTotal',
    );
    expect(
      toFabricRequest(
        { ...fabric, ratios: { '30/1 Penye|lot-1': '33,33', '20 denye likra|': '66,66' } },
        options,
      ),
    ).toBe('ratioTotal');
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
