import type { CentralProductionRequest, YarnReceiptRequest } from '@iplik/api-client/central-stock';
import type { MasterOption } from '@/modules/shipment';
import { parseKg, pctInputToRate, sumKg } from '@iplik/units';

export const MAX_FIRE_PCT = 20;

/** i18n anahtarı (`stock.draftErrors.*`). */
export type StockDraftError = 'required' | 'invalidKg' | 'invalidRate' | 'invalidRollCount';

/** İplik, Tanımlar'daki aktif iplik listesinden seçilir; serbest metin yoktur. */
export type YarnLineDraft = { product: MasterOption | undefined; netKg: string };
export type ReceiptDraft = { product: MasterOption | undefined; netKg: string; note: string };
export const EMPTY_RECEIPT_DRAFT: ReceiptDraft = { product: undefined, netKg: '', note: '' };

export function toReceiptRequest(draft: ReceiptDraft): YarnReceiptRequest | StockDraftError {
  const netKg = parseKg(draft.netKg);
  if (draft.product === undefined) return 'required';
  if (netKg === undefined) return 'invalidKg';
  const note = draft.note.trim();
  return { productId: draft.product.value, netKg, ...(note === '' ? {} : { note }) };
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
  yarns: [{ product: undefined, netKg: '' }],
});

const ROLL_PATTERN = /^[1-9]\d*$/;

function toYarnLines(
  rows: readonly YarnLineDraft[],
): CentralProductionRequest['yarns'] | StockDraftError {
  const lines: CentralProductionRequest['yarns'] = [];
  for (const row of rows) {
    const netKg = parseKg(row.netKg);
    if (row.product === undefined) return 'required';
    if (netKg === undefined) return 'invalidKg';
    lines.push({ productId: row.product.value, netKg });
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
