import { FIRE_SETTLEMENTS_PATH, listFireSettlements } from '@iplik/api-client/settlements';
import { useQuery } from '@tanstack/react-query';

export const TO_INVOICE_PAGE_SIZE = 20;

/** `page` 1 tabanlıdır (arayüz); sunucu 0 tabanlıdır. */
export function useToInvoice(page: number) {
  return useQuery({
    queryKey: [FIRE_SETTLEMENTS_PATH, 'TO_INVOICE', page],
    queryFn: () =>
      listFireSettlements({ status: 'TO_INVOICE', page: page - 1, size: TO_INVOICE_PAGE_SIZE }),
  });
}
