import type { RecipeLineView } from '@iplik/api-client';
import { findRecipe } from '../components/shipmentViews';
import { useShipment } from '../hooks/useShipment';
import type { YarnOption } from './fabricLimits';
import { yarnOptionsOf } from './fabricLimits';

/** Reçete satırının iplik anahtarı (`yarnOptionsOf` ile aynı kural). */
function yarnKeyOf(line: RecipeLineView): string {
  const text = (line.yarnTypeText ?? '').trim();
  return text === '' ? (line.yarnTypeId ?? '') : text;
}

/** Seçilen ipliğin sevkiyatta parti bilgisi girilmiş partileri; hiç yoksa boş. */
function lotOptionsOf(lines: readonly RecipeLineView[], yarnType: string): YarnOption[] {
  const lots = new Map<string, string>();
  for (const line of lines) {
    if (yarnKeyOf(line) !== yarnType || line.lotId === undefined || line.lotId === '') continue;
    lots.set(line.lotId, (line.lotNo ?? '').trim() || line.lotId);
  }
  return [...lots].map(([value, label]) => ({ value, label }));
}

/** Seçenek tek ise değeri, değilse boş. */
const onlyValueOf = (options: readonly YarnOption[]) =>
  options.length === 1 ? (options[0]?.value ?? '') : '';

/** Seçim listede varsa kalır; yoksa tek seçenek kendiliğinden seçilir, değilse boşalır. */
function resolve(picked: string, options: readonly YarnOption[]): string {
  return options.some((option) => option.value === picked) ? picked : onlyValueOf(options);
}

/**
 * Dönen iplik formunun seçenekleri: iplik sevkiyatta gönderilenlerden, parti de seçilen ipliğin sevkiyattaki
 * partilerinden seçilir. Tek seçenek varsa kendiliğinden seçili gelir; geçersiz kalan seçim boşalır.
 */
export function useReturnOptions(shipmentId: string, picked: { yarnType: string; lotId: string }) {
  const shipment = useShipment(shipmentId).data;
  const recipe = shipment === undefined ? undefined : findRecipe(shipment);
  const lines = recipe?.lines ?? [];
  const yarns = yarnOptionsOf(lines);
  const yarnType = resolve(picked.yarnType, yarns);
  const lots = lotOptionsOf(lines, yarnType);
  return { yarns, lots, yarnType, lotId: resolve(picked.lotId, lots) };
}
