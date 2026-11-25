import type { FabricRecipeRequest, RecipeYarn } from '@iplik/api-client/fabric';
import { FULL_HUNDREDTHS, parseRatio, ratioHundredths } from '@iplik/units';

export type RecipeLineDraft = { yarnType: string; ratio: string };
export type RecipeDraft = { fabricType: string; lines: RecipeLineDraft[] };
export const EMPTY_RECIPE_DRAFT: RecipeDraft = {
  fabricType: '',
  lines: [{ yarnType: '', ratio: '' }],
};

/** Yazılan oranların toplamı (yüzde); okunamayan satır sayılmaz. */
export function recipeTotal(draft: RecipeDraft): number {
  let total = 0;
  for (const line of draft.lines) {
    const ratio = parseRatio(line.ratio);
    if (ratio !== undefined) total += ratioHundredths(ratio);
  }
  return total / 100;
}

/** Karışım isteği; kumaş cinsi dolu, her satırda iplik ve geçerli oran olmalı, iplikler tekrarlanmamalı, toplam tam 100. */
export function toRecipeRequest(draft: RecipeDraft): FabricRecipeRequest | undefined {
  const fabricType = draft.fabricType.trim();
  if (fabricType === '' || draft.lines.length === 0) return undefined;
  const yarns: RecipeYarn[] = [];
  let hundredths = 0;
  for (const line of draft.lines) {
    const ratio = parseRatio(line.ratio);
    const yarnType = line.yarnType.trim();
    if (yarnType === '' || ratio === undefined) return undefined;
    if (yarns.some((y) => y.yarnType.toLowerCase() === yarnType.toLowerCase())) return undefined;
    hundredths += ratioHundredths(ratio);
    yarns.push({ yarnType, ratioPct: ratio });
  }
  return hundredths === FULL_HUNDREDTHS ? { fabricType, yarns } : undefined;
}
