import type { TareStandardSaveRequest } from '@iplik/api-client';
import { listTareStandards, saveTareStandard } from '@iplik/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { MASTER_ADMIN_PATH } from '../kinds';

const KEY = [MASTER_ADMIN_PATH, 'tare-standards'] as const;

export function useTareStandards() {
  return useQuery({ queryKey: KEY, queryFn: async () => (await listTareStandards()).items });
}

export function useSaveTareStandard() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: TareStandardSaveRequest) => saveTareStandard(request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: KEY }),
  });
}
