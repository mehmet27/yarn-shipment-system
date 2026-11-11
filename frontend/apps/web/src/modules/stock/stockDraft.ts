import type { CentralProductionRequest, YarnReceiptRequest } from '@iplik/api-client/central-stock';
import { parseKg, pctInputToRate, sumKg } from '@iplik/units';

export const MAX_FIRE_PCT = 20;

/** i18n anahtarı (`stock.draftErrors.*`). */
export type StockDraftError = 'required' | 'invalidKg' | 'invalidRate' | 'invalidRollCount';

export type YarnLineDraft = { yarnType: string; netKg: string };
export type ReceiptDraft = { yarnType: string; netKg: string; note: string };
export const EMPTY_RECEIPT_DRAFT: ReceiptDraft = { yarnType: '', netKg: '', note: '' };

export function toReceiptRequest(draft: ReceiptDraft): YarnReceiptRequest | StockDraftError {
  const netKg = parseKg(draft.netKg);
  if (draft.yarnType.trim() === '') return 'required';
  if (netKg === undefined) return 'invalidKg';
  const note = draft.note.trim();
  return { yarnType: draft.yarnType.trim(), netKg, ...(note === '' ? {} : { note }) };
}

export type ProductionDraft = {
  fabricType: string;
  firePct: string;
  rollCount: string;
  waybillNo: string;
  yarns: YarnLineDraft[];
};
export const emptyProductionDraft = (firePct: string): ProductionDraft => ({
  fabricType: '',
  firePct,
  rollCount: '',
  waybillNo: '',
  yarns: [{ yarnType: '', netKg: '' }],
});

const ROLL_PATTERN = /^[1-9]\d*$/;

function toYarnLines(rows: readonly YarnLineDraft[]): YarnLineDraft[] | StockDraftError {
  const lines: YarnLineDraft[] = [];
  for (const row of rows) {
    const netKg = parseKg(row.netKg);
    if (row.yarnType.trim() === '') return 'required';
    if (netKg === undefined) return 'invalidKg';
    lines.push({ yarnType: row.yarnType.trim(), netKg });
  }
  return lines.length === 0 ? 'required' : lines;
}

/** Kumaş kilosu iplik satırlarının toplamıdır; fire sunucuda stoktan düşülürken eklenir. */
export function toProductionRequest(
  draft: ProductionDraft,
): CentralProductionRequest | StockDraftError {
  const fireRate = pctInputToRate(draft.firePct, MAX_FIRE_PCT);
  if (draft.fabricType.trim() === '') return 'required';
  if (fireRate === undefined) return 'invalidRate';
  const roll = draft.rollCount.trim();
  if (roll !== '' && !ROLL_PATTERN.test(roll)) return 'invalidRollCount';
  const yarns = toYarnLines(draft.yarns);
  if (typeof yarns === 'string') return yarns;
  const waybillNo = draft.waybillNo.trim();
  return {
    fabricType: draft.fabricType.trim(),
    fireRate,
    yarns,
    ...(roll === '' ? {} : { rollCount: Number(roll) }),
    ...(waybillNo === '' ? {} : { waybillNo }),
  };
}

export const productionTotalKg = (draft: ProductionDraft): string =>
  sumKg(draft.yarns.map((row) => parseKg(row.netKg) ?? '0.000'));
