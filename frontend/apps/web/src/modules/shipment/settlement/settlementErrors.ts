import { toProblem } from '@iplik/api-client';

const KNOWN_CODES = [
  'VALIDATION_FAILED',
  'SHIPMENT_NOT_COMPLETED',
  'FIRE_FILE_CLOSED',
  'YARN_DISTRIBUTION_MISMATCH',
  'FIRE_INVOICE_NOT_APPLICABLE',
  'FIRE_CLOSE_NOT_APPLICABLE',
  'ACCESS_DENIED',
  'INVALID_LEDGER_INPUT',
  'FABRIC_YARN_SHORT',
  'FABRIC_YARN_NOT_IN_SHIPMENT',
  'FABRIC_RECIPE_EXISTS',
  'FABRIC_RECIPE_NOT_FOUND',
  'FIRE_SETTLEMENT_CLOSED',
  'INVALID_FIRE_SETTLEMENT_ACTION',
];
const ACTION_REASONS = ['NOTHING_TO_INVOICE', 'INVOICE_EXCEEDS_REMAINING', 'FAULTY_KG_REMAINS'];

/** Bilinen hata kodları Türkçe mesaja (`settlement.errors.*`); bilinmeyenler genel mesaja düşer. */
export function settlementErrorKey(error: unknown): string {
  const { code, reason } = toProblem(error);
  if (
    code === 'INVALID_FIRE_SETTLEMENT_ACTION' &&
    reason !== undefined &&
    ACTION_REASONS.includes(reason)
  ) {
    return `settlement.errors.${reason}`;
  }
  return `settlement.errors.${KNOWN_CODES.includes(code) ? code : 'unknown'}`;
}
