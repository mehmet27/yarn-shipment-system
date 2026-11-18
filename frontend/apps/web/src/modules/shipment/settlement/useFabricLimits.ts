import type { Kilogram } from '@iplik/units';
import type { YarnOption } from './fabricLimits';
import { findRecipe } from '../components/shipmentViews';
import { useShipment } from '../hooks/useShipment';
import { remainingFabricKg, yarnOptionsOf } from './fabricLimits';
import { useFireSettlement } from './hooks';

export type FabricLimits = {
  /** Bu sevkiyattaki iplikler. */
  yarnNames: YarnOption[];
  /** Fire dosyası okunamadıysa tümü undefined; ekran cümleyi gizler. */
  sentKg: string | undefined;
  receivedKg: string | undefined;
  /** Eksik kumaş için faturası kesilen kilo; kumaş gibi sayılır (itirazı karşılayan fatura kısmı sayılmaz). */
  invoicedKg: string | undefined;
  remainingKg: Kilogram | undefined;
};

/** Kumaş formunun sınırları: sevkiyatın iplik adları ile gönderilen, gelen ve kalan kilo. */
export function useFabricLimits(shipmentId: string): FabricLimits {
  const shipment = useShipment(shipmentId).data;
  const settlement = useFireSettlement(shipmentId).data;
  const recipe = shipment === undefined ? undefined : findRecipe(shipment);
  return {
    yarnNames: recipe === undefined ? [] : yarnOptionsOf(recipe.lines),
    sentKg: settlement?.sentKg,
    receivedKg: settlement?.fabricKg,
    invoicedKg: settlement?.shortageInvoicedKg,
    remainingKg:
      settlement === undefined
        ? undefined
        : remainingFabricKg(settlement.sentKg, settlement.fabricKg, settlement.shortageInvoicedKg),
  };
}
