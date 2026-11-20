import type { ShipmentDetail } from '@iplik/api-client';
import { usePermission } from '@/modules/auth';
import { useShipmentBags } from '../hooks/useShipmentBags';
import { useFireSettlement } from '../settlement/hooks';
import { useSettlementAccess } from '../settlement/useSettlementAccess';
import type { ShipmentGuideView } from './guideSteps';
import { toGuideView } from './guideSteps';

/**
 * Sevkiyatın şu anki adımı, sıranın kimde olduğu ve kullanıcının basacağı düğme. Çuval listesi yalnızca taslakta,
 * fire dosyası yalnızca tamamlanmış sevkiyatta ve dosyayı görebilen kullanıcıda okunur; veri gelene kadar boş döner.
 */
export function useShipmentGuide(shipment: ShipmentDetail): ShipmentGuideView | undefined {
  const access = useSettlementAccess();
  const canRecordFabric = usePermission('FABRIC_RECEIPT_RECORD') && access.fabric;
  const canMarkInvoice = usePermission('INVOICE_MARK');
  const isDraft = shipment.status === 'DRAFT';
  const canReadFire = shipment.status === 'COMPLETED' && access.fire;
  const bags = useShipmentBags(shipment.shipmentId, isDraft);
  const fire = useFireSettlement(shipment.shipmentId, canReadFire);
  if ((isDraft && bags.isPending) || (canReadFire && fire.isPending)) return undefined;
  return toGuideView(
    {
      status: shipment.status,
      hasBags: (bags.data?.dispatchBags.length ?? 0) > 0,
      fire: fire.data,
    },
    { actions: shipment.allowedActions, canRecordFabric, canMarkInvoice },
  );
}
