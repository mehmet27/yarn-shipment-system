import type { DiscrepancyView } from '@iplik/api-client';
import { useDiscrepancies } from './useDiscrepancies';

const LIST_SIZE = 20;

/** Vadesi yakın olan önce; vadesizler sona. */
function byDue(a: DiscrepancyView, b: DiscrepancyView): number {
  return (a.dueAt ?? '9999').localeCompare(b.dueAt ?? '9999');
}

/** Karar bekleyen (açık ve incelemedeki) uyuşmazlıklar; liste ucu tek durum süzdüğünden iki sorgu birleştirilir. */
export function usePendingDiscrepancies(): DiscrepancyView[] {
  const open = useDiscrepancies({ status: 'OPEN', size: LIST_SIZE });
  const inReview = useDiscrepancies({ status: 'IN_REVIEW', size: LIST_SIZE });
  return [...(open.data?.items ?? []), ...(inReview.data?.items ?? [])]
    .filter((item) => item.allowedActions?.includes('RESOLVE') ?? false)
    .sort(byDue);
}
