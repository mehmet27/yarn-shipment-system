import { useMutation, useQueryClient } from '@tanstack/react-query';
import { ADAPTERS } from '../adapters';
import type { ManagedKind, RecordValues } from '../kinds';
import { MASTER_ADMIN_PATH } from '../kinds';

export type SaveRequest = { id: string | undefined; values: RecordValues };

/** Kayıttan sonra hem yönetim listeleri hem sevkiyat seçicileri (aynı anahtar öneki) yenilenir. */
export function useSaveRecord(kind: ManagedKind) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, values }: SaveRequest) => ADAPTERS[kind].save(id, values),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [MASTER_ADMIN_PATH] }),
  });
}
