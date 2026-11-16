import type { RecipeLineView } from '@iplik/api-client';
import type { CreateDraft, RecipeLineDraft } from './createDraft';
import { EMPTY_CREATE_DRAFT, newLine } from './createDraft';

/** API'nin ondalık noktalı sayısı ("1050.000"), taslağın Türkçe girdi biçimine ("1050,000") çevrilir. */
function toTurkishInput(value: string | undefined): string {
  return value === undefined ? '' : value.replace('.', ',');
}

function lineDraft(line: RecipeLineView): RecipeLineDraft {
  return {
    ...newLine(),
    netKg: toTurkishInput(line.netKg),
    fabricType: line.fabricType ?? '',
    lycra: line.lycra ?? '',
    widthType: line.widthType ?? '',
    yarnLength: line.yarnLength ?? '',
    pussFine: line.pussFine ?? '',
    unitPrice: toTurkishInput(line.unitPrice),
  };
}

/**
 * Kayıtlı sevkiyatın reçetesinden sipariş formu PDF'inin taslağı. Sipariş no ve yetkili kişi sevkiyatla
 * kaydedilmediği için boş bırakılır.
 */
export function shipmentOrderDraft(
  subcontractorTitle: string,
  lines: readonly RecipeLineView[],
): CreateDraft {
  return {
    ...EMPTY_CREATE_DRAFT,
    subcontractor: { value: '', label: subcontractorTitle },
    lines: lines.map(lineDraft),
  };
}
