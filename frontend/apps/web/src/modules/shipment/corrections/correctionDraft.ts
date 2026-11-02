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
  sealNo: string;
  reason: string;
};

/** i18n anahtarı (`shipment.correction.errors.*`). */
export type CorrectionError =
  'targetRequired' | 'invalidKg' | 'tareExceedsGross' | 'sealNoRequired' | 'reasonRequired';

export const EMPTY_CORRECTION_DRAFT: CorrectionDraft = {
  target: undefined,
  grossKg: '',
  tareKg: '',
  sealNo: '',
  reason: '',
};

/** Yalnızca sevkiyatta girilmiş kayıtlar düzeltilebilir. */
export function correctableTargets(shipment: InternalShipmentView): CorrectionTarget[] {
  const targets: [CorrectionTarget, boolean][] = [
    ['DISPATCH_WEIGHING', shipment.dispatchWeighing !== undefined],
    ['DISPATCH_SEAL_NO', shipment.dispatchSealNo !== undefined],
    ['DELIVERY_WEIGHING', shipment.deliveryWeighing !== undefined],
    ['DELIVERY_SEAL_NO', shipment.deliverySealCondition !== undefined],
  ];
  return targets.filter(([, isRecorded]) => isRecorded).map(([target]) => target);
}

export function isWeighingTarget(target: CorrectionTarget | undefined): boolean {
  return target === 'DISPATCH_WEIGHING' || target === 'DELIVERY_WEIGHING';
}

/** İlk hatayı ya da sözleşmedeki isteği döner. */
export function toCorrectionRequest(
  draft: CorrectionDraft,
): RecordCorrectionRequest | CorrectionError {
  if (draft.target === undefined) return 'targetRequired';
  const reason = draft.reason.trim();
  if (isWeighingTarget(draft.target)) {
    const grossKg = parseKg(draft.grossKg);
    const tareKg = parseKg(draft.tareKg);
    if (grossKg === undefined || tareKg === undefined) return 'invalidKg';
    if (calculateNetKg(grossKg, tareKg) === undefined) return 'tareExceedsGross';
    return reason === '' ? 'reasonRequired' : { target: draft.target, grossKg, tareKg, reason };
  }
  const sealNo = draft.sealNo.trim();
  if (sealNo === '') return 'sealNoRequired';
  return reason === '' ? 'reasonRequired' : { target: draft.target, sealNo, reason };
}

/** Tartım değeri backend'de "gross=… tare=… net=…" biçimindedir; mühür numarası olduğu gibi. */
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
