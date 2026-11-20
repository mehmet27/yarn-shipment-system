import type { DiscrepancyView } from '@iplik/api-client';
import { useDiscrepancies } from './useDiscrepancies';

const LIST_SIZE = 20;

/** Vadesi yakın olan önce; vadesizler sona. */
function byDue(a: DiscrepancyView, b: DiscrepancyView): number {
  return (a.dueAt ?? '9999').localeCompare(b.dueAt ?? '9999');
}

/**
 * Karar bekleyen (açık ve incelemedeki) uyuşmazlıklar; liste ucu tek durum süzdüğünden iki sorgu birleştirilir.
 * `isEnabled` kapalıysa (kullanıcının karar yetkisi yoksa) sorgu atılmaz.
 */
export function usePendingDiscrepancies(isEnabled = true): DiscrepancyView[] {
  const open = useDiscrepancies({ status: 'OPEN', size: LIST_SIZE }, isEnabled);
  const inReview = useDiscrepancies({ status: 'IN_REVIEW', size: LIST_SIZE }, isEnabled);
  return [...(open.data?.items ?? []), ...(inReview.data?.items ?? [])]
    .filter((item) => item.allowedActions?.includes('RESOLVE') ?? false)
    .sort(byDue);
}
