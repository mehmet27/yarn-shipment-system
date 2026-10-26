import { listSites } from '@iplik/api-client';
import { useQuery } from '@tanstack/react-query';
import { MASTER_ADMIN_PATH } from '../kinds';

const SITE_PAGE_SIZE = 100;

/** Hesabın göreceği lokasyonlar için etkin lokasyonlar (sözleşme en çok 100 seçime izin verir). */
export function useSiteChoices() {
  return useQuery({
    queryKey: [MASTER_ADMIN_PATH, 'site-choices'],
    queryFn: async () =>
      (await listSites({ page: 0, size: SITE_PAGE_SIZE })).items.map((site) => ({
        id: site.id,
        name: site.name,
      })),
  });
}
