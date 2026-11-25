import type { RecipeLineView } from '@iplik/api-client';
export type YarnOption = { value: string; label: string };

/**
 * Sevkiyattaki iplikler. `value` sunucunun bakiyede sakladığı anahtardır (yazılan metin, yoksa listeden seçilen
 * ipliğin kimliği); `label` ekranda görünen addır. Aynı iplik bir kez sunulur.
 */
export function yarnOptionsOf(lines: readonly RecipeLineView[]): YarnOption[] {
  const options = new Map<string, string>();
  for (const line of lines) {
    const text = (line.yarnTypeText ?? '').trim();
    const value = text === '' ? (line.yarnTypeId ?? '') : text;
    const label = (line.yarnTypeName ?? '').trim() || text;
    if (value !== '' && label !== '' && !options.has(value)) options.set(value, label);
  }
  return [...options].map(([value, label]) => ({ value, label }));
}
