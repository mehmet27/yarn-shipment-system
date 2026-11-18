import { toProblem } from '@iplik/api-client';
import type { DispatchBagView } from '@iplik/api-client';
import { compareWeighing, sumKg } from '@iplik/units';
import { useState } from 'react';
import { useShipmentBags } from '../hooks/useShipmentBags';
import { useWeightTolerance } from '../hooks/useWeightTolerance';

/** Çuvalların toplamı araç net kilosunu tutmadığında uyarının gösterdiği kilolar (kg metni); `diffKg` mutlak farktır. */
export type BagMismatch = { bagKg: string; measuredKg: string; diffKg: string };

function mismatchOf(
  bagKg: string,
  measuredKg: string,
  tolerance?: string,
): BagMismatch | undefined {
  const comparison = compareWeighing(measuredKg, bagKg, tolerance);
  return comparison?.exceedsTolerance === true
    ? { bagKg, measuredKg, diffKg: comparison.diffKg.replace('-', '') }
    : undefined;
}

/** Çuval toplamı, araç net kilosundan kantar toleransından fazla sapıyorsa iki kiloyu döner (sunucudakiyle aynı kural). */
export function findBagMismatch(
  netKg: string | undefined,
  bags: readonly DispatchBagView[] | undefined,
  tolerance: string | undefined,
): BagMismatch | undefined {
  if (netKg === undefined || bags === undefined) return undefined;
  return mismatchOf(sumKg(bags.map((bag) => bag.weightKg)), netKg, tolerance);
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
    check: (netKg: string | undefined): boolean => {
      const found = findBagMismatch(netKg, bags.data?.dispatchBags, tolerance);
      setMismatch(found);
      return found !== undefined;
    },
    /** Sunucu hatası çuval uyuşmazlığıysa uyarıyı açar ve true döner. */
    reject: (error: unknown): boolean => {
      const { code, bagKg, measuredKg } = toProblem(error);
      if (code !== 'BAG_WEIGHT_MISMATCH' || bagKg === undefined || measuredKg === undefined) {
        return false;
      }
      setMismatch(mismatchOf(bagKg, measuredKg) ?? { bagKg, measuredKg, diffKg: '0.000' });
      return true;
    },
    dismiss: () => {
      setMismatch(undefined);
    },
  };
}
