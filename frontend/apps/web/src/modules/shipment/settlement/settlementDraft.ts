import type {
  InvoiceRequest,
  FireRateRequest,
  YarnReturnRequest,
  YarnShare,
} from '@iplik/api-client/settlements';
import type { FabricReceiptRequest, FabricYarnOption, YarnRatio } from '@iplik/api-client/fabric';
import {
  FULL_HUNDREDTHS,
  parseKg,
  parseRatio,
  pctInputToRate,
  ratioHundredths,
  sumKg,
} from '@iplik/units';
import { ratioKey } from './fabricRatios';

export const MAX_FIRE_PCT = 20;

/** i18n anahtarı (`settlement.draftErrors.*`). */
export type DraftError =
  | 'invalidKg'
  | 'required'
  | 'distributionMismatch'
  | 'invalidRate'
  | 'invalidRollCount'
  | 'invalidRatio'
  | 'ratioMissing'
  | 'ratioTotal'
  | 'invoiceExceeds'
  | 'invoiceKgMissing';

/** Kumaş girişi taslağı; oranlar iplik satırının anahtarıyla (`ratioKey`) tutulur, boş oran "yok" demektir. */
export type FabricDraft = {
  fabricType: string;
  netKg: string;
  rollCount: string;
  waybillNo: string;
  ratios: Record<string, string>;
};
export const EMPTY_FABRIC_DRAFT: FabricDraft = {
  fabricType: '',
  netKg: '',
  rollCount: '',
  waybillNo: '',
  ratios: {},
};

const ROLL_PATTERN = /^[1-9]\d*$/;

function checkFabricFields(draft: FabricDraft): DraftError | undefined {
  if (draft.fabricType.trim() === '' || draft.waybillNo.trim() === '') return 'required';
  if (parseKg(draft.netKg) === undefined) return 'invalidKg';
  const roll = draft.rollCount.trim();
  return roll !== '' && !ROLL_PATTERN.test(roll) ? 'invalidRollCount' : undefined;
}

/** Dolu oran satırlarını isteğe çevirir; bozuk oran `invalidRatio`, toplamı 100 olmayan karışım `ratioTotal`. */
function toRatios(
  draft: FabricDraft,
  options: readonly FabricYarnOption[],
): YarnRatio[] | DraftError {
  const ratios: YarnRatio[] = [];
  const hundredths: number[] = [];
  for (const option of options) {
    const text = (draft.ratios[ratioKey(option)] ?? '').trim();
    if (text === '') continue;
    const ratio = parseRatio(text);
    if (ratio === undefined) return 'invalidRatio';
    hundredths.push(ratioHundredths(ratio));
    ratios.push({
      yarnType: option.yarnType,
      ...(option.lotId === undefined ? {} : { lotId: option.lotId }),
      ratioPct: ratio,
    });
  }
  if (ratios.length === 0) return 'ratioMissing';
  return hundredths.reduce((sum, value) => sum + value, 0) === FULL_HUNDREDTHS
    ? ratios
    : 'ratioTotal';
}

/**
 * Kumaş girişi isteği: kumaş bilgisi ve fasoncudaki iplikler üzerinden yazılan oranlar; oranların toplamı tam 100
 * olmalıdır. Yeterli iplik olup olmadığını sunucu (önizleme ve kayıt) denetler.
 */
export function toFabricRequest(
  draft: FabricDraft,
  options: readonly FabricYarnOption[],
): FabricReceiptRequest | DraftError {
  const failure = checkFabricFields(draft);
  const netKg = parseKg(draft.netKg);
  if (failure !== undefined) return failure;
  if (netKg === undefined) return 'invalidKg';
  const yarns = toRatios(draft, options);
  if (typeof yarns === 'string') return yarns;
  const roll = draft.rollCount.trim();
  return {
    fabricType: draft.fabricType.trim(),
    netKg,
    waybillNo: draft.waybillNo.trim(),
    ...(roll === '' ? {} : { rollCount: Number(roll) }),
    yarns,
  };
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
