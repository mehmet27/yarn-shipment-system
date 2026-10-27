import { StorageUploadError, toProblem } from '@iplik/api-client';

const KNOWN_CODES = [
  'UPLOAD_NOT_ALLOWED',
  'SHIPMENT_NOT_FOUND',
  'SHIPMENT_LOCKED',
  'UPLOAD_NOT_COMPLETED',
  'UPLOAD_MISMATCH',
  'SHIPMENT_SERVICE_UNAVAILABLE',
];

/** Bilinen hata kodları Türkçe mesaja; depo hatası ve bilinmeyenler genel mesaja düşer. */
export function uploadErrorKey(error: unknown): string {
  if (error instanceof StorageUploadError) return 'evidence.errors.storage';
  const { code } = toProblem(error);
  return `evidence.errors.${KNOWN_CODES.includes(code) ? code : 'unknown'}`;
}
