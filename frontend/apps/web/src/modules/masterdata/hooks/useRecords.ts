import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { ADAPTERS } from '../adapters';
import type { ListParams, ManagedKind } from '../kinds';
import { MASTER_ADMIN_PATH } from '../kinds';

export function useRecords(kind: ManagedKind, params: ListParams) {
  return useQuery({
    queryKey: [MASTER_ADMIN_PATH, 'manage', kind, params],
    queryFn: () => ADAPTERS[kind].list(params),
    placeholderData: keepPreviousData,
  });
}
