import type { RecipeLineView } from '@iplik/api-client';
import type { Kilogram } from '@iplik/units';
import { isKilogram, parseKg, subtractKg } from '@iplik/units';

const KG_SCALE = 3;

/** Sevkiyattaki iplik adları (sunucunun sakladığı ad: listeden seçilenin adı, yoksa yazılan metin); tekrarsız. */
export function yarnNamesOf(lines: readonly RecipeLineView[]): string[] {
  const names = lines
    .map((line) => (line.yarnTypeName ?? line.yarnTypeText ?? '').trim())
    .filter((name) => name !== '');
  return [...new Set(names)];
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
