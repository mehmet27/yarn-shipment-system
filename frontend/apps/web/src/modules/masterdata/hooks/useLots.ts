import { listLots, listProducts, listQualityGrades } from '@iplik/api-client';
import type { LotView } from '@iplik/api-client';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { toQuery } from '../catalogAdapters';
import type { ListParams } from '../kinds';
import { MASTER_ADMIN_PATH } from '../kinds';

export const LOTS_KEY = [MASTER_ADMIN_PATH, 'lots'] as const;

const CHOICE_PAGE_SIZE = 100;

export type LotPage = { rows: LotView[]; totalPages: number };

export function useLots(params: ListParams) {
  return useQuery({
    queryKey: [...LOTS_KEY, params],
    queryFn: async (): Promise<LotPage> => {
      const page = await listLots(toQuery(params));
      return { rows: page.items, totalPages: page.page.totalPages };
    },
    placeholderData: keepPreviousData,
  });
}

export type Choice = { value: string; label: string };

/** Parti formundaki iplik ve kalite seçimleri: yalnızca etkin kayıtlar, "KOD · ad" biçiminde. */
export function useCatalogChoices() {
  return useQuery({
    queryKey: [MASTER_ADMIN_PATH, 'catalog-choices'],
    queryFn: async () => {
      const query = { page: 0, size: CHOICE_PAGE_SIZE };
      const [products, grades] = await Promise.all([listProducts(query), listQualityGrades(query)]);
      const toChoice = (row: { id: string; code: string; name: string }): Choice => ({
        value: row.id,
        label: `${row.code} · ${row.name}`,
      });
      return { products: products.items.map(toChoice), grades: grades.items.map(toChoice) };
    },
  });
}
