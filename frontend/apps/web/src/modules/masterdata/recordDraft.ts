import type { ManagedKind } from './kinds';

/** i18n anahtarı (`masterData.errors.*`). */
export type RecordError = 'labelRequired' | 'labelTooLong' | 'invalidPlate';

const MAX_LENGTH: Readonly<Record<ManagedKind, number>> = {
  subcontractor: 200,
  driver: 200,
  vehicle: 20,
  site: 200,
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
