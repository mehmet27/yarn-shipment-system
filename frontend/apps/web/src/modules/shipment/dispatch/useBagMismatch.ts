import { toProblem } from '@iplik/api-client';
import type { DispatchBagView, Problem } from '@iplik/api-client';
import { compareWeighing, sumKg } from '@iplik/units';
import { useState } from 'react';
import { useShipmentBags } from '../hooks/useShipmentBags';
import { useWeightTolerance } from '../hooks/useWeightTolerance';

/**
 * Depoda çözülecek çuval uyuşmazlığı. Önce sayı denetlenir: yazılan çuval sayısı eklenen çuvalları tutmuyor.
 * Sonra kilo: çuvalların toplamı araç net kilosunu tutmuyor (kg metni; `diffKg` mutlak farktır).
 */
export type BagMismatch =
  | { kind: 'count'; enteredCount: number; bagCount: number }
  | { kind: 'weight'; bagKg: string; measuredKg: string; diffKg: string };

function mismatchOf(
  bagKg: string,
  measuredKg: string,
  tolerance?: string,
): BagMismatch | undefined {
  const comparison = compareWeighing(measuredKg, bagKg, tolerance);
  return comparison?.exceedsTolerance === true
    ? { kind: 'weight', bagKg, measuredKg, diffKg: comparison.diffKg.replace('-', '') }
    : undefined;
}

/** Çuval sayısı, sonra çuval toplamı; araç net kilosundan kantar toleransından fazla sapıyorsa iki kiloyu döner (sunucudakiyle aynı kural). */
export function findBagMismatch(
  entered: { netKg: string | undefined; bagCount: number | undefined },
  bags: readonly DispatchBagView[] | undefined,
  tolerance: string | undefined,
): BagMismatch | undefined {
  const { netKg, bagCount } = entered;
  if (bags === undefined) return undefined;
  if (bagCount !== undefined && bagCount !== bags.length) {
    return { kind: 'count', enteredCount: bagCount, bagCount: bags.length };
  }
  if (netKg === undefined) return undefined;
  return mismatchOf(sumKg(bags.map((bag) => bag.weightKg)), netKg, tolerance);
}

/** Sunucunun 409 cevabından uyarı; çuval uyuşmazlığı değilse undefined. */
function mismatchOfProblem(problem: Problem): BagMismatch | undefined {
  const { code, bagKg, measuredKg, enteredCount, bagCount } = problem;
  if (code === 'BAG_COUNT_MISMATCH' && enteredCount !== undefined && bagCount !== undefined) {
    return { kind: 'count', enteredCount, bagCount };
  }
  if (code !== 'BAG_WEIGHT_MISMATCH' || bagKg === undefined || measuredKg === undefined) {
    return undefined;
  }
  return mismatchOf(bagKg, measuredKg) ?? { kind: 'weight', bagKg, measuredKg, diffKg: '0.000' };
}

/**
 * "Çuvallar araç kilosunu tutmuyor" uyarısı: "İleri"de eklenen çuvallarla ön denetim yapılır; çuval listesi eskiyse
 * sunucunun 409 `BAG_WEIGHT_MISMATCH` cevabı aynı uyarıyı açar. Asıl karar sunucudadır.
 */
export function useBagMismatch(shipmentId: string) {
  const bags = useShipmentBags(shipmentId);
  const tolerance = useWeightTolerance();
  const [mismatch, setMismatch] = useState<BagMismatch>();
  return {
    mismatch,
    /** Ön denetim; uyuşmazlık varsa uyarıyı açar ve true döner. */
    check: (entered: { netKg: string | undefined; bagCount: number | undefined }): boolean => {
      const found = findBagMismatch(entered, bags.data?.dispatchBags, tolerance);
      setMismatch(found);
      return found !== undefined;
    },
    /** Sunucu hatası çuval uyuşmazlığıysa uyarıyı açar ve true döner. */
    reject: (error: unknown): boolean => {
      const found = mismatchOfProblem(toProblem(error));
      if (found !== undefined) setMismatch(found);
      return found !== undefined;
    },
    dismiss: () => {
      setMismatch(undefined);
    },
  };
}
