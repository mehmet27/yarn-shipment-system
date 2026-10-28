import type { DeliveryDocument, DeliveryReceiptRequest, SealCondition } from '@iplik/api-client';
import type { TicketOutcome } from '@/modules/evidence';
import type { Kilogram } from '@iplik/units';
import { calculateNetKg, parseKg } from '@iplik/units';

/** Sözleşme sınırı: bir tutanakta en fazla 2000 çuval anahtarı. */
export const MAX_BAG_KEYS = 2000;

export type DocumentDraft = {
  shipmentNo: string;
  date: string;
  netKg: string;
  bagCount: string;
};

/** Sihirbazın ham girdileri; kilo alanları kullanıcının yazdığı Türkçe metin olarak tutulur. */
export type DeliveryDraft = {
  sealCondition: SealCondition | undefined;
  sealNo: string;
  bagKeys: readonly string[];
  grossKg: string;
  tareKg: string;
  bagCount: string;
  ticket: TicketOutcome | undefined;
  document: DocumentDraft;
};

export const DELIVERY_STEPS = ['seal', 'bags', 'weighing', 'document', 'summary'] as const;
export type DeliveryStep = (typeof DELIVERY_STEPS)[number];

/** i18n anahtarı (`delivery.errors.*`). */
export type DraftError =
  | 'sealConditionRequired'
  | 'sealNoRequired'
  | 'bagKeysRequired'
  | 'invalidKg'
  | 'tareExceedsGross'
  | 'invalidBagCount'
  | 'ticketRequired';

export const EMPTY_DRAFT: DeliveryDraft = {
  sealCondition: undefined,
  sealNo: '',
  bagKeys: [],
  grossKg: '',
  tareKg: '',
  bagCount: '',
  ticket: undefined,
  document: { shipmentNo: '', date: '', netKg: '', bagCount: '' },
};

/** Tekrar okutulan anahtar listeye ikinci kez eklenmez; backend de tekrarları bir kez sayar. */
export function addBagKey(keys: readonly string[], input: string): readonly string[] {
  const key = input.trim();
  if (key === '' || keys.includes(key) || keys.length >= MAX_BAG_KEYS) return keys;
  return [...keys, key];
}

function parseCount(input: string): number | undefined {
  const value = Number(input.trim());
  return input.trim() !== '' && Number.isInteger(value) && value >= 1 ? value : undefined;
}

export type ParsedWeighing = { grossKg: Kilogram; tareKg: Kilogram; netKg: Kilogram };

export function parseWeighing(draft: DeliveryDraft): ParsedWeighing | DraftError {
  const grossKg = parseKg(draft.grossKg);
  const tareKg = parseKg(draft.tareKg);
  if (grossKg === undefined || tareKg === undefined) return 'invalidKg';
  const netKg = calculateNetKg(grossKg, tareKg);
  return netKg === undefined ? 'tareExceedsGross' : { grossKg, tareKg, netKg };
}

function validateSeal(draft: DeliveryDraft): DraftError | undefined {
  if (draft.sealCondition === undefined) return 'sealConditionRequired';
  if (draft.sealCondition !== 'MISSING' && draft.sealNo.trim() === '') return 'sealNoRequired';
  return undefined;
}

function validateWeighing(draft: DeliveryDraft): DraftError | undefined {
  const weighing = parseWeighing(draft);
  if (typeof weighing === 'string') return weighing;
  if (parseCount(draft.bagCount) === undefined) return 'invalidBagCount';
  return draft.ticket === undefined ? 'ticketRequired' : undefined;
}

function validateDocument({ netKg, bagCount }: DocumentDraft): DraftError | undefined {
  if (netKg.trim() !== '' && parseKg(netKg) === undefined) return 'invalidKg';
  if (bagCount.trim() !== '' && parseCount(bagCount) === undefined) return 'invalidBagCount';
  return undefined;
}

/** Bir adımdan ileri geçmeden önceki doğrulama; ilk hatayı döner. */
export function validateStep(step: DeliveryStep, draft: DeliveryDraft): DraftError | undefined {
  switch (step) {
    case 'seal':
      return validateSeal(draft);
    case 'bags':
      return draft.bagKeys.length === 0 ? 'bagKeysRequired' : undefined;
    case 'weighing':
      return validateWeighing(draft);
    case 'document':
      return validateDocument(draft.document);
    case 'summary':
      return undefined;
  }
}

/** Boş bırakılan irsaliye alanları istekte hiç yer almaz. */
function toDocument(draft: DocumentDraft): DeliveryDocument | undefined {
  const netKg = parseKg(draft.netKg);
  const bagCount = parseCount(draft.bagCount);
  const document: DeliveryDocument = {
    ...(draft.shipmentNo.trim() === '' ? {} : { shipmentNo: draft.shipmentNo.trim() }),
    ...(draft.date === '' ? {} : { date: draft.date }),
    ...(netKg === undefined ? {} : { netKg }),
    ...(bagCount === undefined ? {} : { bagCount }),
  };
  return Object.keys(document).length === 0 ? undefined : document;
}

/** Tüm adımlar geçerliyse sözleşmedeki isteği kurar. */
export function toDeliveryRequest(draft: DeliveryDraft): DeliveryReceiptRequest | undefined {
  const weighing = parseWeighing(draft);
  const bagCount = parseCount(draft.bagCount);
  const hasInvalidStep = DELIVERY_STEPS.some((step) => validateStep(step, draft) !== undefined);
  if (hasInvalidStep || typeof weighing === 'string' || bagCount === undefined) return undefined;
  if (draft.sealCondition === undefined) return undefined;
  const document = toDocument(draft.document);
  return {
    grossKg: weighing.grossKg,
    tareKg: weighing.tareKg,
    bagCount,
    sealCondition: draft.sealCondition,
    ...(draft.sealCondition === 'MISSING' ? {} : { sealNo: draft.sealNo.trim() }),
    bagUniqueKeys: [...draft.bagKeys],
    scanMethod: 'SCAN',
    ...(document === undefined ? {} : { document }),
  };
}
