import type {
  FabricReceiptRequest,
  InvoiceRequest,
  FireRateRequest,
  YarnReturnRequest,
  YarnShare,
} from '@iplik/api-client/settlements';
import type { Kilogram } from '@iplik/units';
import { parseKg, pctInputToRate, sumKg } from '@iplik/units';
import { exceedsRemaining } from './fabricLimits';

export const MAX_FIRE_PCT = 20;

/** i18n anahtarı (`settlement.draftErrors.*`). */
export type DraftError =
  | 'invalidKg'
  | 'required'
  | 'distributionMismatch'
  | 'invalidRate'
  | 'invalidRollCount'
  | 'exceedsRemaining'
  | 'invoiceExceeds'
  | 'invoiceKgMissing';

export type YarnRowDraft = { yarnType: string; netKg: string };
export type FabricDraft = {
  fabricType: string;
  netKg: string;
  rollCount: string;
  waybillNo: string;
  yarns: YarnRowDraft[];
};
export const EMPTY_FABRIC_DRAFT: FabricDraft = {
  fabricType: '',
  netKg: '',
  rollCount: '',
  waybillNo: '',
  yarns: [],
};

const ROLL_PATTERN = /^[1-9]\d*$/;

function toShares(rows: readonly YarnRowDraft[]): YarnShare[] | DraftError {
  const shares: YarnShare[] = [];
  for (const row of rows) {
    const netKg = parseKg(row.netKg);
    if (row.yarnType.trim() === '') return 'required';
    if (netKg === undefined) return 'invalidKg';
    shares.push({ yarnType: row.yarnType.trim(), netKg });
  }
  return shares;
}

function checkFabricFields(draft: FabricDraft, remainingKg?: Kilogram): DraftError | undefined {
  if (draft.fabricType.trim() === '' || draft.waybillNo.trim() === '') return 'required';
  if (parseKg(draft.netKg) === undefined) return 'invalidKg';
  if (exceedsRemaining(draft.netKg, remainingKg)) return 'exceedsRemaining';
  const roll = draft.rollCount.trim();
  return roll !== '' && !ROLL_PATTERN.test(roll) ? 'invalidRollCount' : undefined;
}

/**
 * Kumaş girişi isteği; iplik satırı yoksa sunucu reçeteyle dağıtır, varsa toplamı kumaş kilosuna eşit olmalı.
 * `remainingKg` bilinirse kumaş kilosu onu aşamaz (sunucu da `FABRIC_EXCEEDS_SENT_YARN` ile reddeder).
 */
export function toFabricRequest(
  draft: FabricDraft,
  remainingKg?: Kilogram,
): FabricReceiptRequest | DraftError {
  const failure = checkFabricFields(draft, remainingKg);
  const netKg = parseKg(draft.netKg);
  if (failure !== undefined) return failure;
  if (netKg === undefined) return 'invalidKg';
  const roll = draft.rollCount.trim();
  const base: FabricReceiptRequest = {
    fabricType: draft.fabricType.trim(),
    netKg,
    waybillNo: draft.waybillNo.trim(),
    ...(roll === '' ? {} : { rollCount: Number(roll) }),
  };
  if (draft.yarns.length === 0) return base;
  const shares = toShares(draft.yarns);
  if (typeof shares === 'string') return shares;
  return sumKg(shares.map((s) => s.netKg)) === sumKg([netKg])
    ? { ...base, yarns: shares }
    : 'distributionMismatch';
}

export type YarnReturnDraft = {
  yarnType: string;
  lotId: string;
  netKg: string;
  waybillNo: string;
};
export const EMPTY_RETURN_DRAFT: YarnReturnDraft = {
  yarnType: '',
  lotId: '',
  netKg: '',
  waybillNo: '',
};

export function toReturnRequest(draft: YarnReturnDraft): YarnReturnRequest | DraftError {
  const netKg = parseKg(draft.netKg);
  if (draft.yarnType.trim() === '') return 'required';
  if (netKg === undefined) return 'invalidKg';
  const lotId = draft.lotId.trim();
  const waybillNo = draft.waybillNo.trim();
  return {
    yarnType: draft.yarnType.trim(),
    netKg,
    ...(lotId === '' ? {} : { lotId }),
    ...(waybillNo === '' ? {} : { waybillNo }),
  };
}

export function toRateRequest(ratePct: string, reason: string): FireRateRequest | DraftError {
  const fireRate = pctInputToRate(ratePct, MAX_FIRE_PCT);
  if (fireRate === undefined) return 'invalidRate';
  return reason.trim() === '' ? 'required' : { fireRate, reason: reason.trim() };
}

const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

export type InvoiceLineDraft = { yarnType: string; netKg: string };
export type InvoiceDraft = {
  invoiceNo: string;
  invoicedOn: string;
  note: string;
  lines: readonly InvoiceLineDraft[];
};

/** Dolu iplik satırlarını paya çevirir; kilo geçersizse ya da hiç kilo yazılmamışsa hata kodu döner. */
function toInvoiceShares(lines: readonly InvoiceLineDraft[]): YarnShare[] | DraftError {
  const shares: YarnShare[] = [];
  for (const line of lines.filter((l) => l.netKg.trim() !== '')) {
    const netKg = parseKg(line.netKg);
    if (netKg === undefined) return 'invalidKg';
    shares.push({ yarnType: line.yarnType, netKg });
  }
  return lines.length > 0 && shares.length === 0 ? 'invoiceKgMissing' : shares;
}

/**
 * "Fatura edildi" isteği. Dolu iplik satırları faturalanan kiloyu verir; toplamı `remainingKg`'ı aşamaz. Hiç satır
 * yoksa (iplik bilgisi yoksa) kalanın tamamı faturalanır.
 */
export function toInvoiceRequest(
  draft: InvoiceDraft,
  remainingKg?: string,
): InvoiceRequest | DraftError {
  if (draft.invoiceNo.trim() === '' || !DATE_PATTERN.test(draft.invoicedOn)) return 'required';
  const shares = toInvoiceShares(draft.lines);
  if (typeof shares === 'string') return shares;
  const total = Number(sumKg(shares.map((share) => share.netKg)));
  if (remainingKg !== undefined && total > Number(remainingKg)) return 'invoiceExceeds';
  const note = draft.note.trim();
  return {
    invoiceNo: draft.invoiceNo.trim(),
    invoicedOn: draft.invoicedOn,
    ...(note === '' ? {} : { note }),
    ...(shares.length === 0 ? {} : { lines: shares }),
  };
}
