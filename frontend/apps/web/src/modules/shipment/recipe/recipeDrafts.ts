import type { RecipeLineView } from '@iplik/api-client';
import type { MasterOption } from '../hooks/useMasterOptions';
import type { RecipeLineDraft } from '../create/createDraft';
import { newLine } from '../create/createDraft';

/** İplik yalnızca Tanımlar'dan seçilir; serbest metinle yazılmış eski satırda seçim boş gelir, müdür listeden seçer. */
function yarnOption(line: RecipeLineView): MasterOption | undefined {
  if (line.yarnTypeId === undefined) return undefined;
  return { value: line.yarnTypeId, label: line.yarnTypeName ?? line.yarnTypeText ?? '' };
}

function lotOption(line: RecipeLineView): MasterOption | undefined {
  return line.lotId === undefined ? undefined : { value: line.lotId, label: line.lotNo ?? '' };
}

/** Sipariş formu alanları; kayıtta yoksa boş metin. */
function orderFields(line: RecipeLineView) {
  return {
    fabricType: line.fabricType ?? '',
    lycra: line.lycra ?? '',
    widthType: line.widthType ?? '',
    yarnLength: line.yarnLength ?? '',
    pussFine: line.pussFine ?? '',
    unitPrice: (line.unitPrice ?? '').replace('.', ','),
  };
}

/** Kayıtlı reçete satırını düzenlenebilir taslağa çevirir (kilo ve fiyat Türkçe yazımla). */
export function draftFromLine(line: RecipeLineView): RecipeLineDraft {
  return {
    ...newLine(),
    yarn: yarnOption(line),
    lot: lotOption(line),
    netKg: line.netKg.replace('.', ','),
    ...orderFields(line),
  };
}
