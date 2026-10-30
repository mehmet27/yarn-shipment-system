import { toProblem } from '@iplik/api-client';

/** Backend `QUALITY_CLAIM_*` kodları (discrepancy-service ClaimRejection) ve servis hataları. */
const KNOWN_CODES = [
  'QUALITY_CLAIM_NOT_AVAILABLE',
  'QUALITY_CLAIM_WINDOW_CLOSED',
  'QUALITY_CLAIM_BAG_NOT_DELIVERED',
  'QUALITY_CLAIM_BAG_ALREADY_CLAIMED',
  'QUALITY_CLAIM_EVIDENCE_REQUIRED',
  'QUALITY_CLAIM_EVIDENCE_INVALID',
  'QUALITY_CLAIM_INSPECTION_RECORDED',
  'QUALITY_CLAIM_RESULT_EVIDENCE_REQUIRED',
  'DISCREPANCY_NOT_OPEN',
  'EVIDENCE_SERVICE_UNAVAILABLE',
];

/** Bilinen hata kodları Türkçe mesaja (`claim.errors.*`); bilinmeyenler genel mesaja düşer. */
export function claimErrorKey(error: unknown): string {
  const { code } = toProblem(error);
  return `claim.errors.${KNOWN_CODES.includes(code) ? code : 'unknown'}`;
}

export function isNotAvailable(error: unknown): boolean {
  return toProblem(error).code === 'QUALITY_CLAIM_NOT_AVAILABLE';
}
