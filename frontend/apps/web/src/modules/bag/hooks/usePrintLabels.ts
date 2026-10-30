import type { PrintLabelsRequest } from '@iplik/api-client';
import { printBagLabels } from '@iplik/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { LOTS_QUERY_KEY } from './useLockedLots';

/** Baskıdan sonra baskı sayıları yeniden okunur. */
export function usePrintLabels() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: PrintLabelsRequest) => printBagLabels(request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: LOTS_QUERY_KEY }),
  });
}
