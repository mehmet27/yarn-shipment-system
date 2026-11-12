import type { ManagedKind } from './kinds';

/** i18n anahtarı (`masterData.errors.*`). */
export type RecordError =
  | 'labelRequired'
  | 'labelTooLong'
  | 'invalidPlate'
  | 'codeRequired'
  | 'invalidCode'
  | 'extraTooLong'
  | 'invalidSortOrder';

const MAX_LENGTH: Readonly<Record<ManagedKind, number>> = {
  subcontractor: 200,
  driver: 200,
  vehicle: 20,
  product: 200,
  qualityGrade: 200,
  yarnSupplier: 200,
};

/** TR plaka: il kodu 01-81, 1-3 harf, 2-4 rakam (ör. 34 ABC 123). */
const PLATE_PATTERN = /^(0[1-9]|[1-7]\d|8[01])[A-Z]{1,3}\d{2,4}$/;

/** Plaka boşluksuz ve büyük harfle saklanır; "i" Türkçe "İ" değil "I" olur (plakada İ yoktur). */
export function normalizeLabel(kind: ManagedKind, label: string): string {
  const trimmed = label.trim();
  return kind === 'vehicle' ? trimmed.replaceAll(/\s+/g, '').toUpperCase() : trimmed;
}

export function validateLabel(kind: ManagedKind, label: string): RecordError | undefined {
  const trimmed = label.trim();
  if (trimmed === '') return 'labelRequired';
  if (trimmed.length > MAX_LENGTH[kind]) return 'labelTooLong';
  if (kind === 'vehicle' && !PLATE_PATTERN.test(normalizeLabel(kind, trimmed))) {
    return 'invalidPlate';
  }
  return undefined;
}

/** Backend `MasterDataCode`: harf/rakamla başlar; harf, rakam ve `+ . _ / -`; en çok 40 karakter. */
const CODE_PATTERN = /^[A-Z0-9][A-Z0-9+._/-]{0,39}$/;
const SORT_ORDER_PATTERN = /^\d{1,4}$/;
const EXTRA_MAX_LENGTH = 50;

/** Kod sunucuda da büyük harfe çevrilir; "i" Türkçe "İ" değil "I" olur (desende yalnızca ASCII var). */
export function normalizeCode(code: string): string {
  return code.trim().toUpperCase();
}

export function validateCode(code: string): RecordError | undefined {
  const normalized = normalizeCode(code);
  if (normalized === '') return 'codeRequired';
  return CODE_PATTERN.test(normalized) ? undefined : 'invalidCode';
}

/** İplik numarası serbest metindir; kalite sınıfı sırası 0-9999 arası tam sayıdır. Boş bırakılabilir. */
export function normalizeExtra(extra: string): string | undefined {
  const trimmed = extra.trim();
  return trimmed === '' ? undefined : trimmed;
}

export function validateExtra(kind: ManagedKind, extra: string): RecordError | undefined {
  const normalized = normalizeExtra(extra);
  if (normalized === undefined) return undefined;
  if (kind === 'qualityGrade') {
    return SORT_ORDER_PATTERN.test(normalized) ? undefined : 'invalidSortOrder';
  }
  return normalized.length > EXTRA_MAX_LENGTH ? 'extraTooLong' : undefined;
}
