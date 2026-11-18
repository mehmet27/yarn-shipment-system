import type { RecipeLineView } from '@iplik/api-client';

export function yarnLabel(line: RecipeLineView): string {
  return line.yarnTypeName ?? line.yarnTypeText ?? line.yarnTypeId ?? '—';
}

/** Sipariş formu ayrıntıları: LYC, en türü, iplik uzunluğu, puss-fine ve fiyat; girilmeyenler atlanır. */
export function specText(line: RecipeLineView, t: (key: string) => string): string {
  const parts = [
    line.lycra,
    line.widthType === undefined ? undefined : t(`create.recipe.widthTypes.${line.widthType}`),
    line.yarnLength,
    line.pussFine,
    line.unitPrice === undefined ? undefined : `${line.unitPrice} TL/kg`,
  ].filter((part): part is string => part !== undefined && part !== '');
  return parts.length === 0 ? '—' : parts.join(' · ');
}
