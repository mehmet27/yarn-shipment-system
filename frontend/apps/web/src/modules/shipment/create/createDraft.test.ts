import { describe, expect, it } from 'vitest';
import {
  EMPTY_CREATE_DRAFT,
  newLine,
  toCreateRequest,
  totalNetKg,
  validateCreate,
  validateCreateFields,
  withSubcontractor,
} from './createDraft';

const site = { value: 'site-merkez', label: 'Merkez Depo' };
const subcontractor = { value: 'sub-1', label: 'Bursa Tekstil' };
const line = { ...newLine(), yarnText: '20 denye likra', netKg: '2.036' };
const base = { ...EMPTY_CREATE_DRAFT, site, subcontractor, lines: [line] };

describe('createDraft', () => {
  it('lokasyon ve fasoncu zorunludur', () => {
    expect(validateCreate(EMPTY_CREATE_DRAFT)).toBe('siteRequired');
    expect(validateCreate({ ...EMPTY_CREATE_DRAFT, site })).toBe('subcontractorRequired');
    expect(validateCreate(base)).toBeUndefined();
  });

  it('şoför ve araç boşsa istekte yer almaz', () => {
    expect(toCreateRequest(base)).toEqual({
      siteId: 'site-merkez',
      subcontractorId: 'sub-1',
      subcontractorTitle: 'Bursa Tekstil',
      lines: [{ netKg: '2036.000', yarnTypeText: '20 denye likra' }],
      fireRate: '0.0400',
    });
  });

  it('şoför ve araç seçilmişse kimlik ve adı birlikte gider', () => {
    const draft = {
      ...base,
      driver: { value: 'drv-1', label: 'Mert Kaya' },
      vehicle: { value: 'veh-0', label: '34 ABC 123' },
    };
    expect(toCreateRequest(draft)).toMatchObject({
      driverId: 'drv-1',
      driverName: 'Mert Kaya',
      vehicleId: 'veh-0',
      plateNo: '34 ABC 123',
    });
  });

  it('fasoncu değişince şoför temizlenir', () => {
    const draft = { ...EMPTY_CREATE_DRAFT, driver: { value: 'drv-1', label: 'Mert Kaya' } };
    expect(withSubcontractor(draft, subcontractor).driver).toBeUndefined();
  });

  it('en az bir geçerli reçete satırı gerekir', () => {
    expect(validateCreate({ ...base, lines: [] })).toBe('linesRequired');
    expect(validateCreate({ ...base, lines: [newLine()] })).toBe('lineYarnRequired');
    expect(validateCreate({ ...base, lines: [{ ...line, netKg: '0' }] })).toBe('lineKgInvalid');
    expect(validateCreate({ ...base, lines: [{ ...line, netKg: '' }] })).toBe('lineKgInvalid');
  });

  it('satır liste kimliği ya da serbest metin, lot isteğe bağlı gider', () => {
    const request = toCreateRequest({
      ...base,
      lines: [
        {
          ...newLine(),
          yarn: { value: 'prd-1', label: 'Viskon' },
          lot: { value: 'lot-1', label: 'L1' },
          netKg: '19.488',
        },
        line,
      ],
    });
    expect(request?.lines).toEqual([
      { yarnTypeId: 'prd-1', lotId: 'lot-1', netKg: '19488.000' },
      { yarnTypeText: '20 denye likra', netKg: '2036.000' },
    ]);
  });

  it('fire oranı yüzdeden kesire çevrilir ve 0-20 dışı reddedilir', () => {
    expect(toCreateRequest({ ...base, fireRatePct: '4,5' })?.fireRate).toBe('0.0450');
    expect(toCreateRequest(base, '3')?.fireRate).toBe('0.0300');
    expect(validateCreateFields({ ...base, fireRatePct: '21' }).fireRate).toBe('fireRateInvalid');
    expect(validateCreateFields({ ...base, fireRatePct: '4,123' }).fireRate).toBe(
      'fireRateInvalid',
    );
    expect(validateCreateFields({ ...base, fireRatePct: '0' }).fireRate).toBeUndefined();
  });

  it('canlı toplam geçerli satırları toplar', () => {
    expect(
      totalNetKg([
        { ...line, netKg: '19.488' },
        { ...line, netKg: '13.492' },
        { ...line, netKg: 'x' },
      ]),
    ).toBe('32980.000');
  });

  it('sipariş formu alanları doluysa satırla gider, boşsa hiç yer almaz', () => {
    const full = {
      ...line,
      fabricType: '30/150 Penye Siyah',
      lycra: '30 denye',
      widthType: 'OPEN_WIDTH',
      yarnLength: '100/31-11-11',
      pussFine: '32/28',
      unitPrice: '21,5',
    };
    expect(toCreateRequest({ ...base, lines: [full] })?.lines[0]).toEqual({
      netKg: '2036.000',
      yarnTypeText: '20 denye likra',
      fabricType: '30/150 Penye Siyah',
      lycra: '30 denye',
      widthType: 'OPEN_WIDTH',
      yarnLength: '100/31-11-11',
      pussFine: '32/28',
      unitPrice: '21.5',
    });
  });

  it('geçersiz fiyat satırı hatalı yapar', () => {
    expect(validateCreate({ ...base, lines: [{ ...line, unitPrice: '-3' }] })).toBe(
      'linePriceInvalid',
    );
  });
});
