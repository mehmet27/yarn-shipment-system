import { allPages } from '../allPages';
import { listLots, listProducts, listQualityGrades, listYarnSuppliers } from '@iplik/api-client';
import type { LotView } from '@iplik/api-client';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { toQuery } from '../catalogAdapters';
import type { ListParams } from '../kinds';
import { MASTER_ADMIN_PATH } from '../kinds';

export const LOTS_KEY = [MASTER_ADMIN_PATH, 'lots'] as const;
/** Sevkiyat formundaki lot seçicisinin önbellek anahtarı (`useMasterOptions`, kind = 'lot'). */
export const LOT_OPTIONS_KEY = [MASTER_ADMIN_PATH, 'lot'] as const;

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
      const [products, grades, suppliers] = await Promise.all([
        allPages(listProducts),
        allPages(listQualityGrades),
        allPages(listYarnSuppliers),
      ]);
      const toChoice = (row: { id: string; code: string; name: string }): Choice => ({
        value: row.id,
        label: `${row.code} · ${row.name}`,
      });
      return {
        products: products.map((row) => ({ value: row.id, label: row.name })),
        grades: grades.map(toChoice),
        suppliers: suppliers.map((row) => ({ value: row.id, label: row.name })),
      };
    },
  });
}
