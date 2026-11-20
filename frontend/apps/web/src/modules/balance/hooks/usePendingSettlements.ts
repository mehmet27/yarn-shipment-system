import type { FireSettlementRow, FireStatus } from '@iplik/api-client/settlements';
import { FIRE_SETTLEMENTS_PATH, listFireSettlements } from '@iplik/api-client/settlements';
import { useQuery } from '@tanstack/react-query';
import { useCurrentUser, usePermission } from '@/modules/auth';

/** Ana sayfadaki iş listesinde gösterilen satır sayısı; toplam ayrıca yazılır. */
export const PENDING_SHOWN = 5;

export type PendingSettlements = {
  /** Kullanıcının bu işi yapma yetkisi var ve liste henüz yüklenmedi. */
  isLoading: boolean;
  items: readonly FireSettlementRow[];
  /** Bekleyen dosyaların tamamı (gösterilenden fazla olabilir). */
  total: number;
};

function usePendingSettlements(status: FireStatus, isEnabled: boolean): PendingSettlements {
  const query = useQuery({
    queryKey: [FIRE_SETTLEMENTS_PATH, status, 'pending'],
    queryFn: () => listFireSettlements({ status, page: 0, size: PENDING_SHOWN }),
    enabled: isEnabled,
  });
  return {
    isLoading: isEnabled && query.isPending,
    items: query.data?.items ?? [],
    total: query.data?.page.totalElements ?? 0,
  };
}

/** Kumaş girişi yapan kalite: iplik teslim edilmiş, henüz hiç kumaş girilmemiş sevkiyatlar. Merkez kalite görmez. */
export function usePendingFabric(): PendingSettlements {
  const { data: user } = useCurrentUser();
  const canRecord = usePermission('FABRIC_RECEIPT_RECORD');
  const canList = usePermission('BALANCE_VIEW');
  return usePendingSettlements(
    'AWAITING_FABRIC',
    canRecord && canList && user?.workArea !== 'CENTRAL',
  );
}

/** Müdür: faturası girilecek ya da kapatılacak fire dosyaları. */
export function usePendingInvoices(): PendingSettlements {
  const canMark = usePermission('INVOICE_MARK');
  const canList = usePermission('BALANCE_VIEW');
  return usePendingSettlements('TO_INVOICE', canMark && canList);
}
