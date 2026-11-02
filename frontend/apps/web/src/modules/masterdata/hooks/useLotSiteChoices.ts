import { useCurrentUser } from '@/modules/auth';
import type { Choice } from './useLots';
import { useSiteChoices } from './useSiteChoices';

/** Yönetici her lokasyona, kalite kullanıcısı yalnızca kendi lokasyonlarına parti açar (backend da denetler). */
export function useLotSiteChoices(): Choice[] {
  const { data: user } = useCurrentUser();
  const { data: sites = [] } = useSiteChoices();
  const isAllSites = user?.permissions.includes('MASTER_DATA_MANAGE') ?? false;
  const allowed = new Set(user?.siteIds ?? []);
  return sites
    .filter((site) => isAllSites || allowed.has(site.id))
    .map((site) => ({ value: site.id, label: site.name }));
}
