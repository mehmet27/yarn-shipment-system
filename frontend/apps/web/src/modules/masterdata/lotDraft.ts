import { normalizeCode, validateCode } from './recordDraft';

/** Parti formu; seçimler kimlik, tarih `YYYY-MM-DD`. Lokasyon yalnızca eklerken seçilir. */
export type LotDraft = {
  siteId: string;
  lotNo: string;
  productId: string;
  qualityGradeId: string;
  productionDate: string;
};

export type LotField = keyof LotDraft;

/** i18n anahtarı (`lots.errors.*`). */
export type LotError = 'required' | 'invalidLotNo' | 'futureDate';

export const EMPTY_LOT: LotDraft = {
  siteId: '',
  lotNo: '',
  productId: '',
  qualityGradeId: '',
  productionDate: '',
};

export const LOT_FIELDS: readonly LotField[] = [
  'siteId',
  'lotNo',
  'productId',
  'qualityGradeId',
  'productionDate',
];

/** Parti numarası kod kuralına uyar ve büyük harfe çevrilir (backend `MasterDataCode`). */
export const normalizeLotNo = normalizeCode;

/** Boş alanlar, geçersiz parti numarası ve bugünden sonraki üretim tarihi; `today` yerel `YYYY-MM-DD`. */
export function validateLot(draft: LotDraft, today: string): Partial<Record<LotField, LotError>> {
  const errors: Partial<Record<LotField, LotError>> = {};
  for (const field of LOT_FIELDS) {
    if (draft[field].trim() === '') errors[field] = 'required';
  }
  if (errors.lotNo === undefined && validateCode(draft.lotNo) !== undefined) {
    errors.lotNo = 'invalidLotNo';
  }
  if (errors.productionDate === undefined && draft.productionDate > today) {
    errors.productionDate = 'futureDate';
  }
  return errors;
}

/** Tarayıcının yerel günü; üretim tarihi ileri bir gün olamaz. */
export function localToday(now: Date = new Date()): string {
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${String(now.getFullYear())}-${month}-${day}`;
}
