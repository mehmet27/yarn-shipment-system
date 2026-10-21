import { describe, expect, it } from 'vitest';
import {
  EMPTY_CREATE_DRAFT,
  toCreateRequest,
  validateCreate,
  withSubcontractor,
} from './createDraft';

const site = { value: 'site-merkez', label: 'Merkez Depo' };
const subcontractor = { value: 'sub-1', label: 'Bursa Tekstil' };

describe('createDraft', () => {
  it('lokasyon ve fasoncu zorunludur', () => {
    expect(validateCreate(EMPTY_CREATE_DRAFT)).toBe('siteRequired');
    expect(validateCreate({ ...EMPTY_CREATE_DRAFT, site })).toBe('subcontractorRequired');
    expect(validateCreate({ ...EMPTY_CREATE_DRAFT, site, subcontractor })).toBeUndefined();
  });

  it('şoför ve araç boşsa istekte yer almaz', () => {
    expect(toCreateRequest({ ...EMPTY_CREATE_DRAFT, site, subcontractor })).toEqual({
      siteId: 'site-merkez',
      subcontractorId: 'sub-1',
      subcontractorTitle: 'Bursa Tekstil',
    });
  });

  it('şoför ve araç seçilmişse kimlik ve adı birlikte gider', () => {
    const draft = {
      site,
      subcontractor,
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
});
