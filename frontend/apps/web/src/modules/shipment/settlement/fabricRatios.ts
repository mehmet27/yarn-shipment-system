import type { FabricRecipe, FabricYarnOption } from '@iplik/api-client/fabric';
import { parseRatio, ratioHundredths } from '@iplik/units';

const HUNDRED = 100;

/** Oran satırının anahtarı: aynı iplik farklı partilerde ayrı satırdır. */
export function ratioKey(option: Pick<FabricYarnOption, 'yarnType' | 'lotId'>): string {
  return `${option.yarnType}|${option.lotId ?? ''}`;
}

/** Yazılan oranların toplamı (yüzde); okunamayan satır sayılmaz. */
export function ratioTotal(ratios: Record<string, string>): number {
  let total = 0;
  for (const text of Object.values(ratios)) {
    const ratio = parseRatio(text);
    if (ratio !== undefined) total += ratioHundredths(ratio);
  }
  return total / HUNDRED;
}

const sameName = (a: string, b: string) => a.trim().toLowerCase() === b.trim().toLowerCase();

/** Kumaş cinsine uyan (büyük/küçük harf fark etmez) tanımlı karışım. */
export function findRecipe(
  recipes: readonly FabricRecipe[],
  fabricType: string,
): FabricRecipe | undefined {
  const name = fabricType.trim();
  return name === '' ? undefined : recipes.find((recipe) => sameName(recipe.fabricType, name));
}

/**
 * Karışımı satırlara yazar. Fasoncuda aynı iplik birden çok partideyse oran partilere elde bulunan kiloya göre
 * bölünür; yuvarlama farkı en çok kiloya sahip partiye eklenir. Fasoncuda olmayan iplik yazılamaz.
 */
export function recipeToRatios(
  recipe: FabricRecipe,
  options: readonly FabricYarnOption[],
): Record<string, string> {
  const ratios: Record<string, string> = {};
  for (const line of recipe.yarns) {
    const rows = options.filter((option) => sameName(option.yarnType, line.yarnType));
    const weights = rows.map((row) => Math.max(Number(row.totalKg), 0));
    const sum = weights.reduce((total, weight) => total + weight, 0);
    if (rows.length === 0) continue;
    const target = ratioHundredths(line.ratioPct);
    const shares = weights.map((weight) =>
      sum === 0 ? Math.floor(target / rows.length) : Math.floor((target * weight) / sum),
    );
    const largest = weights.indexOf(Math.max(...weights));
    shares[largest] = (shares[largest] ?? 0) + target - shares.reduce((a, b) => a + b, 0);
    rows.forEach((row, index) => {
      const share = shares[index] ?? 0;
      if (share > 0) ratios[ratioKey(row)] = String(share / HUNDRED);
    });
  }
  return ratios;
}
