import type { PackagingType, TareStandardSaveRequest } from '@iplik/api-client';

export const PACKAGING_TYPES: readonly PackagingType[] = ['BAG', 'BOX', 'PALLET', 'BOBBIN'];

export type TareStandardDraft = {
  siteId: string;
  packagingType: PackagingType;
  standardTareKg: string;
  maxDeviationPct: string;
};

export type TareStandardErrors = Partial<
  Record<'siteId' | 'standardTareKg' | 'maxDeviationPct', true>
>;

const KG_SCALE = 3;
const PCT_SCALE = 2;
const MAX_PCT = 100;
const KG_PATTERN = /^\d{1,6}([.,]\d{1,3})?$/;
const PCT_PATTERN = /^\d{1,3}([.,]\d{1,2})?$/;

/** Virgül ondalık ayracı kabul edilir; sözleşme noktalı üç/iki ondalık ister. */
function normalize(value: string, scale: number): string {
  const [whole = '0', fraction = ''] = value.trim().replace(',', '.').split('.');
  return `${whole}.${fraction.padEnd(scale, '0')}`;
}

export function validateTareStandard(draft: TareStandardDraft): TareStandardErrors {
  const errors: TareStandardErrors = {};
  if (draft.siteId === '') errors.siteId = true;
  if (!KG_PATTERN.test(draft.standardTareKg.trim())) errors.standardTareKg = true;
  const pct = draft.maxDeviationPct.trim();
  if (!PCT_PATTERN.test(pct) || Number(pct.replace(',', '.')) > MAX_PCT) {
    errors.maxDeviationPct = true;
  }
  return errors;
}

export function toRequest(draft: TareStandardDraft): TareStandardSaveRequest {
  return {
    siteId: draft.siteId,
    packagingType: draft.packagingType,
    standardTareKg: normalize(draft.standardTareKg, KG_SCALE),
    maxDeviationPct: normalize(draft.maxDeviationPct, PCT_SCALE),
  };
}
