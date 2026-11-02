import type { ManagedKind } from './kinds';

/** i18n anahtarı (`masterData.errors.*`). */
export type RecordError =
  | 'labelRequired'
  | 'labelTooLong'
  | 'invalidPlate'
  | 'codeRequired'
  | 'invalidCode'
  | 'extraTooLong'
  | 'invalidColor';

const MAX_LENGTH: Readonly<Record<ManagedKind, number>> = {
  subcontractor: 200,
  driver: 200,
  vehicle: 20,
  site: 200,
  product: 200,
  qualityGrade: 200,
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
const COLOR_PATTERN = /^#[0-9A-F]{6}$/;
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

/** İplik numarası serbest metindir; etiket rengi `#RRGGBB`. Boş bırakılabilir. */
export function normalizeExtra(kind: ManagedKind, extra: string): string | undefined {
  const trimmed = extra.trim();
  if (trimmed === '') return undefined;
  return kind === 'qualityGrade' ? trimmed.toUpperCase() : trimmed;
}

export function validateExtra(kind: ManagedKind, extra: string): RecordError | undefined {
  const normalized = normalizeExtra(kind, extra);
  if (normalized === undefined) return undefined;
  if (kind === 'qualityGrade') return COLOR_PATTERN.test(normalized) ? undefined : 'invalidColor';
  return normalized.length > EXTRA_MAX_LENGTH ? 'extraTooLong' : undefined;
}
