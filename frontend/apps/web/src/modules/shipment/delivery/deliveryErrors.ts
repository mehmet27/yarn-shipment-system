import { toProblem } from '@iplik/api-client';

const BAD_GATEWAY = 502;

/** Tutanak gönderim hatası → i18n anahtarı; kanıt doğrulama hataları ayrı mesaj alır. */
export function deliveryFailureKey(error: unknown): string {
  const { code, status } = toProblem(error);
  if (code === 'INVALID_SHIPMENT_STATUS') return 'delivery.invalidStatus';
  if (code === 'DELIVERY_EVIDENCE_INVALID') return 'delivery.evidenceInvalid';
  return status === BAD_GATEWAY ? 'delivery.evidenceUnavailable' : 'delivery.submitFailed';
}
