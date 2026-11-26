import { listLots } from '@iplik/api-client';
import { useDebouncedValue } from '@iplik/ui';
import { useQuery } from '@tanstack/react-query';
import { MASTER_DATA_PATH } from '@/modules/shipment';
import { lotState, lotSuggestions } from '../lotChoice';
import type { ReceiptDraft } from '../stockDraft';
import { LOT_NO_PATTERN } from '../stockDraft';

const SEARCH_DEBOUNCE_MS = 300;
const LOT_PAGE_SIZE = 100;

/** Parti no alanının önerileri ve durumu; liste yazılan numarayla sunucuda aranır. */
export function useLotChoice({ lotNo, product, supplier }: ReceiptDraft) {
  const typed = lotNo.trim().toUpperCase();
  const [q] = useDebouncedValue(typed, SEARCH_DEBOUNCE_MS);
  // Parti numarası olamayacak metin (çok uzun, geçersiz karakter) sunucuda aranmaz; arama ucu bunu 400 ile reddeder.
  const isSearchable = q === '' || LOT_NO_PATTERN.test(q);
  const query = useQuery({
    enabled: isSearchable,
    queryKey: [MASTER_DATA_PATH, 'lot', 'receipt', q],
    queryFn: async () =>
      (await listLots({ size: LOT_PAGE_SIZE, ...(q === '' ? {} : { q }) })).items,
  });
  const owner = { productId: product?.value, supplierId: supplier?.value };
  return {
    suggestions: lotSuggestions(query.data ?? [], owner),
    state: lotState(typed, q === typed ? query.data : undefined, owner),
  };
}
