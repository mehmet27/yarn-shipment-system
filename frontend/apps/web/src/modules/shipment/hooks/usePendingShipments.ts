import type { ShipmentAction, ShipmentStatus, ShipmentSummary } from '@iplik/api-client';
import { listShipments, SHIPMENTS_PATH } from '@iplik/api-client';
import { useQuery } from '@tanstack/react-query';
import { usePermission } from '@/modules/auth';

const LIST_SIZE = 20;

export type PendingShipments = {
  /** Kullanıcının bu işi yapma yetkisi var ve liste henüz yüklenmedi. */
  isLoading: boolean;
  items: readonly ShipmentSummary[];
};

/** Verilen durumdaki sevkiyatlardan, sunucunun bu kullanıcıya verilen işlemi açtıkları. */
function usePendingShipments(
  status: ShipmentStatus,
  action: ShipmentAction,
  isEnabled: boolean,
): PendingShipments {
  const params = { status: [status], size: LIST_SIZE };
  const query = useQuery({
    queryKey: [SHIPMENTS_PATH, params],
    queryFn: () => listShipments(params),
    enabled: isEnabled,
  });
  return {
    isLoading: isEnabled && query.isPending,
    items: (query.data?.items ?? []).filter((item) => item.allowedActions.includes(action)),
  };
}

/** Depo: çuvalı eklenip çıkış tartısı girilecek taslaklar (sevkiyat günü gelmiş olanlar). */
export function usePendingDispatches(): PendingShipments {
  return usePendingShipments('DRAFT', 'RECORD_DISPATCH', usePermission('DISPATCH_RECORD'));
}

/** Müdür: yolda olan, fasoncuya ulaşınca irsaliyeyle tamamlanacak sevkiyatlar. */
export function usePendingDeliveries(): PendingShipments {
  return usePendingShipments('AWAITING_DELIVERY', 'COMPLETE', usePermission('SHIPMENT_COMPLETE'));
}
