import { listProducts, listSubcontractors } from '@iplik/api-client';
import { useQuery } from '@tanstack/react-query';
import { allPages } from '../allPages';
import { MASTER_ADMIN_PATH } from '../kinds';

/** Karışım formundaki iplik adları: etkin ipliklerin tamamı. */
export const useYarnNames = () =>
  useQuery({
    queryKey: [MASTER_ADMIN_PATH, 'product', 'all-names'],
    queryFn: async () => [...new Set((await allPages(listProducts)).map((row) => row.name))],
  });

/** Şoför formundaki fasoncu listesi: etkin fasoncuların tamamı. */
export const useOwnerChoices = () =>
  useQuery({
    queryKey: [MASTER_ADMIN_PATH, 'subcontractor', 'all-choices'],
    queryFn: async () =>
      (await allPages(listSubcontractors)).map((row) => ({ value: row.id, label: row.title })),
  });
