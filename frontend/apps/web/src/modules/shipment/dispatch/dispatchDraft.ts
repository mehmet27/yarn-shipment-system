import type { DispatchWeighingRequest } from '@iplik/api-client';
import type { TicketOutcome } from '@/modules/evidence';
import type { Kilogram } from '@iplik/units';
import { calculateNetKg, parseKg } from '@iplik/units';

/** Kantar fişinden elle girilen ham değerler; kilolar kullanıcının yazdığı Türkçe metindir. */
export type DispatchDraft = {
  grossKg: string;
  tareKg: string;
  bagCount: string;
  scaleTicketNo: string;
  sealNo: string;
  ticket: TicketOutcome | undefined;
};

/** i18n anahtarı (`dispatch.errors.*`). */
export type DispatchError =
  'invalidKg' | 'tareExceedsGross' | 'invalidBagCount' | 'sealNoRequired' | 'ticketRequired';

export const EMPTY_DISPATCH_DRAFT: DispatchDraft = {
  grossKg: '',
  tareKg: '',
  bagCount: '',
  scaleTicketNo: '',
  sealNo: '',
  ticket: undefined,
};

export type DispatchWeighing = { grossKg: Kilogram; tareKg: Kilogram; netKg: Kilogram };

export function parseDispatchWeighing(draft: DispatchDraft): DispatchWeighing | DispatchError {
  const grossKg = parseKg(draft.grossKg);
  const tareKg = parseKg(draft.tareKg);
  if (grossKg === undefined || tareKg === undefined) return 'invalidKg';
  const netKg = calculateNetKg(grossKg, tareKg);
  return netKg === undefined ? 'tareExceedsGross' : { grossKg, tareKg, netKg };
}

function parseBagCount(input: string): number | undefined {
  const value = Number(input.trim());
  return input.trim() !== '' && Number.isInteger(value) && value >= 1 ? value : undefined;
}

/** İlk hatayı döner; hata yoksa undefined. */
export function validateDispatch(draft: DispatchDraft): DispatchError | undefined {
  const weighing = parseDispatchWeighing(draft);
  if (typeof weighing === 'string') return weighing;
  if (parseBagCount(draft.bagCount) === undefined) return 'invalidBagCount';
  if (draft.sealNo.trim() === '') return 'sealNoRequired';
  return draft.ticket === undefined ? 'ticketRequired' : undefined;
}

/** Tüm alanlar geçerliyse sözleşmedeki isteği kurar; boş kantar fişi isteğe girmez. */
export function toDispatchRequest(draft: DispatchDraft): DispatchWeighingRequest | undefined {
  const weighing = parseDispatchWeighing(draft);
  const bagCount = parseBagCount(draft.bagCount);
  if (typeof weighing === 'string' || bagCount === undefined) return undefined;
  const sealNo = draft.sealNo.trim();
  if (draft.ticket === undefined) return undefined;
  const scaleTicketNo = draft.scaleTicketNo.trim();
  if (sealNo === '') return undefined;
  return {
    grossKg: weighing.grossKg,
    tareKg: weighing.tareKg,
    bagCount,
    sealNo,
    ...(scaleTicketNo === '' ? {} : { scaleTicketNo }),
  };
}
