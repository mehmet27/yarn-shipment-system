import { compareWeighing } from '@iplik/units';
import { useWeightTolerance } from '../hooks/useWeightTolerance';
import type { DispatchDraft } from './dispatchDraft';
import { parseDispatchWeighing } from './dispatchDraft';

/** Girilen tartının net kilosu ve (beklenen net biliniyorsa) beklenenle karşılaştırması; geçersiz girdide ikisi de undefined. */
export function useDispatchWeighing(draft: DispatchDraft, plannedNetKg: string | undefined) {
  const tolerance = useWeightTolerance();
  const parsed = parseDispatchWeighing(draft);
  const weighing = typeof parsed === 'string' ? undefined : parsed;
  const comparison =
    weighing === undefined || plannedNetKg === undefined
      ? undefined
      : compareWeighing(weighing.netKg, plannedNetKg, tolerance);
  return { weighing, comparison };
}
