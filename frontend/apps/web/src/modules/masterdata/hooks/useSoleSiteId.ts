import { listSites } from '@iplik/api-client';
import { useQuery } from '@tanstack/react-query';
import { MASTER_ADMIN_PATH } from '../kinds';

/**
 * Firmanın tek lokasyonu (merkez fabrika). Formlarda lokasyon seçtirilmez; istekler bu kimlikle gider.
 * Lokasyon henüz yüklenmediyse `undefined` döner.
 */
export function useSoleSiteId(): string | undefined {
  const { data } = useQuery({
    queryKey: [MASTER_ADMIN_PATH, 'sole-site'],
    queryFn: async () => (await listSites({ page: 0, size: 1 })).items[0]?.id ?? null,
    staleTime: Infinity,
  });
  return data ?? undefined;
}
