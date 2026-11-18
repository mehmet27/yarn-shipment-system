import type { RecipeLineView } from '@iplik/api-client';
import type { Kilogram } from '@iplik/units';
import { isKilogram, parseKg, subtractKg } from '@iplik/units';

const KG_SCALE = 3;

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

/** Gönderilen iplikten gelen kumaş çıkınca girilebilecek kalan kilo; 0'ın altına inmez, sayı geçersizse undefined. */
export function remainingFabricKg(sentKg: string, receivedKg: string): Kilogram | undefined {
  if (!isKilogram(sentKg) || !isKilogram(receivedKg)) return undefined;
  const rest = subtractKg(sentKg, receivedKg);
  const text = rest.lt(0) ? '0.000' : rest.toFixed(KG_SCALE);
  return isKilogram(text) ? text : undefined;
}

/** Girilen net kilo kalan kilodan büyük mü? Kilo okunamıyorsa ya da sınır yoksa false. */
export function exceedsRemaining(netKgInput: string, remainingKg: Kilogram | undefined): boolean {
  const netKg = parseKg(netKgInput);
  return remainingKg !== undefined && netKg !== undefined && subtractKg(netKg, remainingKg).gt(0);
}
