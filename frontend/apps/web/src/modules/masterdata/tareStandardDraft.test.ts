import { describe, expect, it } from 'vitest';
import type { TareStandardDraft } from './tareStandardDraft';
import { toRequest, validateTareStandard } from './tareStandardDraft';

const DRAFT: TareStandardDraft = {
  packagingType: 'BAG',
  standardTareKg: '0,45',
  maxDeviationPct: '5',
};

describe('validateTareStandard', () => {
  it('geçerli taslakta hata yoktur; virgül ve nokta kabul edilir', () => {
    expect(validateTareStandard(DRAFT)).toEqual({});
    expect(
      validateTareStandard({ ...DRAFT, standardTareKg: '0.450', maxDeviationPct: '100' }),
    ).toEqual({});
  });

  it('harfli ve aralık dışı değerleri işaretler', () => {
    expect(validateTareStandard({ ...DRAFT, standardTareKg: 'abc' })).toEqual({
      standardTareKg: true,
    });
    expect(validateTareStandard({ ...DRAFT, standardTareKg: '0.4505' })).toEqual({
      standardTareKg: true,
    });
    expect(validateTareStandard({ ...DRAFT, maxDeviationPct: '100,5' })).toEqual({
      maxDeviationPct: true,
    });
    expect(validateTareStandard({ ...DRAFT, maxDeviationPct: '-1' })).toEqual({
      maxDeviationPct: true,
    });
  });
});

describe('toRequest', () => {
  it('sözleşmedeki üç ve iki ondalık biçime çevirir', () => {
    expect(toRequest(DRAFT, 'site-1')).toEqual({
      siteId: 'site-1',
      packagingType: 'BAG',
      standardTareKg: '0.450',
      maxDeviationPct: '5.00',
    });
    expect(
      toRequest({ ...DRAFT, standardTareKg: '2', maxDeviationPct: '2,5' }, 'site-1'),
    ).toMatchObject({
      standardTareKg: '2.000',
      maxDeviationPct: '2.50',
    });
  });
});
