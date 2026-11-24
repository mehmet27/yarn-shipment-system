import type { DiscrepancyView, ShipmentSummary } from '@iplik/api-client';
import { getYarnVsFabricReport, listDiscrepancies, REPORTS_PATH } from '@iplik/api-client';
import { FIRE_SETTLEMENTS_PATH, listFireSettlements } from '@iplik/api-client/settlements';
import type { FireSettlementRow, FireStatus } from '@iplik/api-client/settlements';
import { todayIsoDate } from '@iplik/i18n';
import { useQuery } from '@tanstack/react-query';
import { usePermission } from '@/modules/auth';
import { fetchAllShipments, useMasterOptions } from '@/modules/shipment';
import { useBalances } from './useBalances';

/** Sunucunun izin verdiği en büyük sayfa; bir fasoncunun bekleyen dosyası bunu aşmaz. */
const PAGE_SIZE = 100;
const MS_PER_DAY = 86_400_000;
/** Fire oranı için bakılan süre: son altı ay (yaklaşık 26 hafta). */
const FIRE_WINDOW_DAYS = 182;
const OPEN_STATUSES = ['OPEN', 'IN_REVIEW'] as const;

function useSettlements(subcontractorId: string, status: FireStatus) {
  return useQuery({
    queryKey: [FIRE_SETTLEMENTS_PATH, status, 'subcontractor', subcontractorId],
    queryFn: async (): Promise<readonly FireSettlementRow[]> =>
      (await listFireSettlements({ status, subcontractorId, page: 0, size: PAGE_SIZE })).items,
  });
}

/** Fasoncunun sevkiyatları: liste fasoncu adına göre aranır, aynı adı taşıyan başka fasoncu kimlikle elenir. */
function useSubcontractorShipments(subcontractorId: string, title: string | undefined) {
  return useQuery({
    queryKey: ['subcontractor-shipments', subcontractorId, title],
    queryFn: async (): Promise<ShipmentSummary[]> =>
      (await fetchAllShipments({ search: title ?? '' })).filter(
        (shipment) => shipment.subcontractorId === subcontractorId,
      ),
    enabled: title !== undefined,
  });
}

function useOpenDiscrepancies(isEnabled: boolean) {
  return useQuery({
    queryKey: ['subcontractor-open-discrepancies'],
    queryFn: async (): Promise<DiscrepancyView[]> => {
      const pages = await Promise.all(
        OPEN_STATUSES.map((status) => listDiscrepancies({ status, page: 0, size: PAGE_SIZE })),
      );
      return pages.flatMap((page) => page.items);
    },
    enabled: isEnabled,
  });
}

function useFireRate(subcontractorId: string, isEnabled: boolean) {
  const to = todayIsoDate();
  const from = todayIsoDate(new Date(Date.now() - FIRE_WINDOW_DAYS * MS_PER_DAY));
  return useQuery({
    queryKey: [REPORTS_PATH, 'yarn-vs-fabric', { from, to }],
    queryFn: () => getYarnVsFabricReport({ from, to }),
    enabled: isEnabled,
    select: (rows) => rows.find((row) => row.subcontractorId === subcontractorId)?.actualFireRate,
  });
}

/** Fasoncunun adı: tanımlardan, bulunamazsa bakiye satırından. */
function useSubcontractorTitle(subcontractorId: string, fallback: string | undefined) {
  const names = useMasterOptions('subcontractor', '');
  return names.data?.find((option) => option.value === subcontractorId)?.label ?? fallback;
}

/** Açık uyuşmazlıklar fasoncuya göre süzülemez; fasoncunun sevkiyatlarına ait olanlar ayıklanır. */
function useSubcontractorDiscrepancies(shipments: readonly ShipmentSummary[]) {
  const canSee = usePermission('DISCREPANCY_VIEW');
  const discrepancies = useOpenDiscrepancies(canSee);
  const shipmentIds = new Set(shipments.map((shipment) => shipment.shipmentId));
  return {
    canSee,
    items: (discrepancies.data ?? []).filter((item) => shipmentIds.has(item.shipmentId)),
  };
}

/**
 * Fasoncu sayfasının verisi: bakiye, yoldaki sevkiyatlar, kumaşı beklenen ve faturalanacak dosyalar, son altı ayın
 * fire oranı ve açık uyuşmazlıklar. Hepsi var olan uçlardan toplanır; kişinin yetkisi olmayan bölüm sorulmaz.
 */
export function useSubcontractorOverview(subcontractorId: string) {
  const canSeeReports = usePermission('REPORT_VIEW');
  const balances = useBalances(subcontractorId);
  const title = useSubcontractorTitle(subcontractorId, balances.data?.items[0]?.subcontractorTitle);
  const shipments = useSubcontractorShipments(subcontractorId, title).data ?? [];
  const discrepancies = useSubcontractorDiscrepancies(shipments);
  return {
    title,
    balances,
    inTransit: shipments.filter((shipment) => shipment.status === 'AWAITING_DELIVERY'),
    awaitingFabric: useSettlements(subcontractorId, 'AWAITING_FABRIC').data ?? [],
    toInvoice: useSettlements(subcontractorId, 'TO_INVOICE').data ?? [],
    fireRate: useFireRate(subcontractorId, canSeeReports).data,
    canSeeDiscrepancies: discrepancies.canSee,
    openDiscrepancies: discrepancies.items,
  };
}

export type SubcontractorOverview = ReturnType<typeof useSubcontractorOverview>;
