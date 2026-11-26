import type { CentralProductionRequest, YarnReceiptRequest } from '@iplik/api-client/central-stock';
import type { MasterOption } from '@/modules/shipment';
import {
  FULL_HUNDREDTHS,
  parseKg,
  parseRatio,
  pctInputToRate,
  ratioHundredths,
} from '@iplik/units';

export const MAX_FIRE_PCT = 20;

/** i18n anahtarı (`stock.draftErrors.*`). */
export type StockDraftError =
  | 'required'
  | 'invalidKg'
  | 'invalidRate'
  | 'invalidRollCount'
  | 'invalidLotNo'
  | 'invalidWaybill'
  | 'invalidRatio'
  | 'ratioTotal'
  | 'duplicateYarn';

/** İplik, Tanımlar'daki aktif iplik listesinden seçilir; `ratio` kumaşın yüzde kaçının bu iplik olduğudur. */
export type YarnLineDraft = { product: MasterOption | undefined; ratio: string };
/** Parti numarası yazılırsa partiye bağlanır; parti ilk kez yazılıyorsa kalite sınıfı da seçilir. */
export type ReceiptDraft = {
  product: MasterOption | undefined;
  supplier: MasterOption | undefined;
  netKg: string;
  lotNo: string;
  qualityGrade: MasterOption | undefined;
  waybillNo: string;
  note: string;
};
export const EMPTY_RECEIPT_DRAFT: ReceiptDraft = {
  product: undefined,
  supplier: undefined,
  netKg: '',
  lotNo: '',
  qualityGrade: undefined,
  waybillNo: '',
  note: '',
};

/** Sunucudaki parti numarası kuralının aynısı: harf/rakamla başlar, en çok 40 karakter, yalnızca A-Z 0-9 + . _ / -. */
export const LOT_NO_PATTERN = /^[A-Z0-9][A-Z0-9+._/-]{0,39}$/;
const MAX_WAYBILL_LENGTH = 128;

/** Parti, irsaliye ve not: boş olanlar isteğe yazılmaz; geçersizse hata anahtarı. */
function toReceiptOptionals(
  draft: ReceiptDraft,
): Pick<YarnReceiptRequest, 'lotNo' | 'qualityGradeId' | 'waybillNo' | 'note'> | StockDraftError {
  const lotNo = draft.lotNo.trim().toUpperCase();
  if (lotNo !== '' && !LOT_NO_PATTERN.test(lotNo)) return 'invalidLotNo';
  const waybillNo = draft.waybillNo.trim();
  if (waybillNo.length > MAX_WAYBILL_LENGTH) return 'invalidWaybill';
  const note = draft.note.trim();
  return {
    ...(lotNo === '' ? {} : { lotNo }),
    ...(lotNo === '' || draft.qualityGrade === undefined
      ? {}
      : { qualityGradeId: draft.qualityGrade.value }),
    ...(waybillNo === '' ? {} : { waybillNo }),
    ...(note === '' ? {} : { note }),
  };
}

export function toReceiptRequest(draft: ReceiptDraft): YarnReceiptRequest | StockDraftError {
  const netKg = parseKg(draft.netKg);
  if (draft.product === undefined || draft.supplier === undefined) return 'required';
  if (netKg === undefined) return 'invalidKg';
  const optionals = toReceiptOptionals(draft);
  if (typeof optionals === 'string') return optionals;
  return { productId: draft.product.value, supplierId: draft.supplier.value, netKg, ...optionals };
}

export type ProductionDraft = {
  fabricType: string;
  netKg: string;
  firePct: string;
  rollCount: string;
  waybillNo: string;
  yarns: YarnLineDraft[];
};
export const emptyProductionDraft = (firePct: string): ProductionDraft => ({
  fabricType: '',
  netKg: '',
  firePct,
  rollCount: '',
  waybillNo: '',
  yarns: [{ product: undefined, ratio: '' }],
});

const ROLL_PATTERN = /^[1-9]\d*$/;

const GRAMS = 1000;
const KG_DECIMALS = 3;
const HUNDRED = 100;

/**
 * Toplam kumaş kilosunu oranlara göre gram gram böler; yuvarlama farkı en büyük orana eklenir, böylece satırların
 * toplamı yazılan kiloya tam eşit olur. `hundredths` toplamı 10.000 olmalıdır.
 */
export function splitKg(netKg: string, hundredths: readonly number[]): string[] {
  const total = Math.round(Number(netKg) * GRAMS);
  const grams = hundredths.map((h) => Math.floor((total * h) / FULL_HUNDREDTHS));
  const largest = hundredths.indexOf(Math.max(...hundredths));
  grams[largest] = (grams[largest] ?? 0) + total - grams.reduce((a, b) => a + b, 0);
  return grams.map((g) => (g / GRAMS).toFixed(KG_DECIMALS));
}

function toYarnLines(
  netKg: string,
  rows: readonly YarnLineDraft[],
): CentralProductionRequest['yarns'] | StockDraftError {
  const ids: string[] = [];
  const hundredths: number[] = [];
  for (const row of rows) {
    if (row.product === undefined || row.ratio.trim() === '') return 'required';
    const ratio = parseRatio(row.ratio);
    if (ratio === undefined) return 'invalidRatio';
    if (ids.includes(row.product.value)) return 'duplicateYarn';
    ids.push(row.product.value);
    hundredths.push(ratioHundredths(ratio));
  }
  if (hundredths.reduce((a, b) => a + b, 0) !== FULL_HUNDREDTHS) return 'ratioTotal';
  const shares = splitKg(netKg, hundredths);
  if (shares.some((kg) => Number(kg) <= 0)) return 'invalidKg';
  return ids.map((productId, i) => ({ productId, netKg: shares[i] ?? '' }));
}

/** İplik başına kumaş kilosu = toplam kumaş × oran; fire sunucuda stoktan düşülürken eklenir. */
export function toProductionRequest(
  draft: ProductionDraft,
): CentralProductionRequest | StockDraftError {
  const fireRate = pctInputToRate(draft.firePct, MAX_FIRE_PCT);
  if (draft.fabricType.trim() === '' || draft.netKg.trim() === '') return 'required';
  const netKg = parseKg(draft.netKg);
  if (netKg === undefined) return 'invalidKg';
  if (fireRate === undefined) return 'invalidRate';
  const roll = draft.rollCount.trim();
  if (roll !== '' && !ROLL_PATTERN.test(roll)) return 'invalidRollCount';
  const yarns = toYarnLines(netKg, draft.yarns);
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

/** Yazılan oranların toplamı (yüzde); okunamayan satır sayılmaz. */
export function productionRatioTotal(draft: ProductionDraft): number {
  let total = 0;
  for (const row of draft.yarns) {
    const ratio = parseRatio(row.ratio);
    if (ratio !== undefined) total += ratioHundredths(ratio);
  }
  return total / HUNDRED;
}

/** Satırın kumaş kilosu ve fire dahil stoktan düşecek iplik (yalnızca ekranda bilgi; hesabı sunucu yapar). */
export function productionShare(
  draft: ProductionDraft,
  row: YarnLineDraft,
): { fabricKg: string; yarnKg: string } | undefined {
  const netKg = parseKg(draft.netKg);
  const ratio = parseRatio(row.ratio);
  const fireRate = pctInputToRate(draft.firePct, MAX_FIRE_PCT);
  if (netKg === undefined || ratio === undefined || fireRate === undefined) return undefined;
  const fabric = (Number(netKg) * Number(ratio)) / HUNDRED;
  return {
    fabricKg: fabric.toFixed(KG_DECIMALS),
    yarnKg: (fabric / (1 - Number(fireRate))).toFixed(KG_DECIMALS),
  };
}
