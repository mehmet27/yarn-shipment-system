import { toProblem } from '@iplik/api-client';

const KNOWN_CODES = [
  'VALIDATION_FAILED',
  'SHIPMENT_NOT_COMPLETED',
  'FIRE_FILE_CLOSED',
  'YARN_DISTRIBUTION_MISMATCH',
  'FIRE_INVOICE_NOT_APPLICABLE',
  'FIRE_CLOSE_NOT_APPLICABLE',
  'ACCESS_DENIED',
];

/** Bilinen hata kodları Türkçe mesaja (`settlement.errors.*`); bilinmeyenler genel mesaja düşer. */
export function settlementErrorKey(error: unknown): string {
  const { code } = toProblem(error);
  return `settlement.errors.${KNOWN_CODES.includes(code) ? code : 'unknown'}`;
}
