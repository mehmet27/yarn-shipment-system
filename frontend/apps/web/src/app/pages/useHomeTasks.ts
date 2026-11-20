import { useIsFetching } from '@tanstack/react-query';
import { usePermission } from '@/modules/auth';
import { usePendingFabric, usePendingInvoices } from '@/modules/balance';
import { usePendingDiscrepancies } from '@/modules/discrepancy';
import {
  usePendingDeliveries,
  usePendingDispatches,
  usePendingWeightDecisions,
} from '@/modules/shipment';

/** "Bekleyen iş yok" kutusunun cümlesi kullanıcının işine göre seçilir. */
export type HomeDuty = 'DISPATCH_RECORD' | 'SHIPMENT_CREATE' | 'FABRIC_RECEIPT_RECORD' | 'default';

export type HomeTasks = {
  /** Kullanıcının ana sayfada takip edeceği bir iş türü var mı (yönetici ve genel müdürde yoktur). */
  hasDuty: boolean;
  isLoading: boolean;
  /** Bütün iş listelerindeki bekleyen iş sayısı. */
  total: number;
  duty: HomeDuty;
};

function toDuty(flags: { canDispatch: boolean; canCreate: boolean; canFabric: boolean }): HomeDuty {
  if (flags.canDispatch) return 'DISPATCH_RECORD';
  if (flags.canCreate) return 'SHIPMENT_CREATE';
  return flags.canFabric ? 'FABRIC_RECEIPT_RECORD' : 'default';
}

/**
 * Ana sayfadaki iş listelerinin toplamı. Listeler kendi kartlarıyla aynı sorguları kullanır (önbellek ortak, istek
 * iki kez atılmaz); burası yalnızca "hiç bekleyen iş yok" durumunu söyleyebilmek için sayar.
 */
export function useHomeTasks(): HomeTasks {
  const canDecide = usePermission('DISCREPANCY_DECIDE');
  const flags = {
    canDispatch: usePermission('DISPATCH_RECORD'),
    canCreate: usePermission('SHIPMENT_CREATE'),
    canFabric: usePermission('FABRIC_RECEIPT_RECORD'),
  };
  const canView = usePermission('SHIPMENT_VIEW');
  const lists = [usePendingDispatches(), usePendingDeliveries()];
  const files = [usePendingFabric(), usePendingInvoices()];
  const decisions = usePendingWeightDecisions({ isEnabled: canView });
  const discrepancies = usePendingDiscrepancies(canDecide);
  // Yalnızca ilk yüklemeler sayılır; arka plandaki yenilemeler "iş yok" kutusunu titretmez.
  const firstLoads = useIsFetching({ predicate: (query) => query.state.data === undefined });
  const total =
    decisions.length +
    discrepancies.length +
    lists.reduce((sum, list) => sum + list.items.length, 0) +
    files.reduce((sum, file) => sum + file.total, 0);
  return {
    hasDuty: canView && (canDecide || Object.values(flags).some(Boolean)),
    isLoading: firstLoads > 0,
    total,
    duty: toDuty(flags),
  };
}
