import { listShipments, SHIPMENTS_PATH } from '@iplik/api-client';
import { useQuery } from '@tanstack/react-query';

const LIST_SIZE = 20;
const PARAMS = { status: ['WEIGHT_MISMATCH'], size: LIST_SIZE } as const;

/** Kantar uyuşmazlığında, giriş yapan kullanıcının karar verebildiği sevkiyatlar; `pollMs` verilirse yenilenir. */
export function usePendingWeightDecisions(pollMs?: number) {
  const query = useQuery({
    queryKey: [SHIPMENTS_PATH, { status: ['WEIGHT_MISMATCH'], size: LIST_SIZE }],
    queryFn: () => listShipments({ status: [...PARAMS.status], size: PARAMS.size }),
    ...(pollMs === undefined ? {} : { refetchInterval: pollMs }),
  });
  return (query.data?.items ?? []).filter((item) =>
    item.allowedActions.includes('RESOLVE_WEIGHT_MISMATCH'),
  );
}
