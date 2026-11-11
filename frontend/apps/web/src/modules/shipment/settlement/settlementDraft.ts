import type {
  FabricReceiptRequest,
  InvoiceRequest,
  FireRateRequest,
  YarnReturnDirection,
  YarnReturnRequest,
  YarnShare,
} from '@iplik/api-client/settlements';
import { parseKg, pctInputToRate, sumKg } from '@iplik/units';

export const MAX_FIRE_PCT = 20;

/** i18n anahtarı (`settlement.draftErrors.*`). */
export type DraftError =
  'invalidKg' | 'required' | 'distributionMismatch' | 'invalidRate' | 'invalidRollCount';

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

/** Kumaş girişi isteği; iplik satırı yoksa sunucu reçeteyle dağıtır, varsa toplamı kumaş kilosuna eşit olmalı. */
export function toFabricRequest(draft: FabricDraft): FabricReceiptRequest | DraftError {
  const netKg = parseKg(draft.netKg);
  if (draft.fabricType.trim() === '' || draft.waybillNo.trim() === '') return 'required';
  if (netKg === undefined) return 'invalidKg';
  const roll = draft.rollCount.trim();
  if (roll !== '' && !ROLL_PATTERN.test(roll)) return 'invalidRollCount';
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
  direction: YarnReturnDirection;
  yarnType: string;
  lotId: string;
  netKg: string;
  waybillNo: string;
};
export const EMPTY_RETURN_DRAFT: YarnReturnDraft = {
  direction: 'RETURN',
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
    direction: draft.direction,
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

export function toInvoiceRequest(
  invoiceNo: string,
  invoicedOn: string,
  note: string,
): InvoiceRequest | DraftError {
  if (invoiceNo.trim() === '' || !DATE_PATTERN.test(invoicedOn)) return 'required';
  return {
    invoiceNo: invoiceNo.trim(),
    invoicedOn,
    ...(note.trim() === '' ? {} : { note: note.trim() }),
  };
}
