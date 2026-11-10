import type {
  CorrectionTarget,
  InternalShipmentView,
  RecordCorrectionRequest,
} from '@iplik/api-client';
import { calculateNetKg, formatKg, isKilogram, parseKg } from '@iplik/units';

export const MAX_REASON_LENGTH = 500;

/** Kullanıcının yazdığı ham değerler; kilolar Türkçe metindir. */
export type CorrectionDraft = {
  target: CorrectionTarget | undefined;
  grossKg: string;
  tareKg: string;
  reason: string;
};

/** i18n anahtarı (`shipment.correction.errors.*`). */
export type CorrectionError =
  'targetRequired' | 'invalidKg' | 'tareExceedsGross' | 'reasonRequired';

export const EMPTY_CORRECTION_DRAFT: CorrectionDraft = {
  target: undefined,
  grossKg: '',
  tareKg: '',
  reason: '',
};

/** Yalnızca sevkiyatta girilmiş kayıtlar düzeltilebilir; bugün tek hedef çıkış tartımıdır. */
export function correctableTargets(shipment: InternalShipmentView): CorrectionTarget[] {
  return shipment.dispatchWeighing === undefined ? [] : ['DISPATCH_WEIGHING'];
}

/** İlk hatayı ya da sözleşmedeki isteği döner. */
export function toCorrectionRequest(
  draft: CorrectionDraft,
): RecordCorrectionRequest | CorrectionError {
  if (draft.target === undefined) return 'targetRequired';
  const reason = draft.reason.trim();
  const grossKg = parseKg(draft.grossKg);
  const tareKg = parseKg(draft.tareKg);
  if (grossKg === undefined || tareKg === undefined) return 'invalidKg';
  if (calculateNetKg(grossKg, tareKg) === undefined) return 'tareExceedsGross';
  return reason === '' ? 'reasonRequired' : { target: draft.target, grossKg, tareKg, reason };
}

/** Tartım değeri backend'de "gross=… tare=… net=…" biçimindedir; başka biçim olduğu gibi yazılır. */
const WEIGHING_PATTERN = /^gross=(\S+) tare=(\S+) net=(\S+)$/;

export function formatCorrectionValue(
  t: (key: string) => string,
  value: string | undefined,
): string {
  if (value === undefined) return '—';
  const match = WEIGHING_PATTERN.exec(value);
  if (match === null) return value;
  const kg = (part: string | undefined) =>
    part !== undefined && isKilogram(part) ? formatKg(part) : (part ?? '');
  return [
    `${t('shipment.file.gross')} ${kg(match[1])}`,
    `${t('shipment.file.tare')} ${kg(match[2])}`,
    `${t('shipment.file.net')} ${kg(match[3])}`,
  ].join(' · ');
}
