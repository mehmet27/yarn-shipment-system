import { useMutation, useQueryClient } from '@tanstack/react-query';
import { ADAPTERS } from '../adapters';
import type { ManagedKind } from '../kinds';
import { MASTER_ADMIN_PATH } from '../kinds';

export type ActiveRequest = { id: string; isActive: boolean };

export function useSetActive(kind: ManagedKind) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, isActive }: ActiveRequest) => ADAPTERS[kind].setActive(id, isActive),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [MASTER_ADMIN_PATH] }),
  });
}
