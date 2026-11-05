import { listUserNames } from '@iplik/api-client/audit';
import { useQuery } from '@tanstack/react-query';

const MAX_IDS = 100;
const FIVE_MINUTES_MS = 300_000;

/** Denetim satırlarındaki kullanıcı kimliklerinin ad soyadı (kimlik → ad); çözülemezse harita boş kalır. */
export function useUserNames(userIds: readonly string[]) {
  const ids = [...new Set(userIds)].sort().slice(0, MAX_IDS);
  return useQuery({
    queryKey: ['user-names', ids],
    queryFn: async (): Promise<ReadonlyMap<string, string>> => {
      const result = await listUserNames({ ids });
      return new Map(result.items.map((item) => [item.id, item.name]));
    },
    enabled: ids.length > 0,
    staleTime: FIVE_MINUTES_MS,
    retry: false,
  });
}
