import { listSubcontractors } from '@iplik/api-client';
import { useQuery } from '@tanstack/react-query';
import { shortId } from '../format';

const PAGE_SIZE = 100;
const NAMES_STALE_MS = 300_000;
/** Çok büyük tanım listelerinde sonsuz döngüyü önler; aşılırsa kalan kimlikler kısaltılmış gösterilir. */
const MAX_PAGES = 20;

type Page<T> = { items: T[]; page: { totalPages: number } };

async function fetchAll<T>(fetchPage: (page: number) => Promise<Page<T>>): Promise<T[]> {
  const items: T[] = [];
  for (let page = 0; page < MAX_PAGES; page += 1) {
    const result = await fetchPage(page);
    items.push(...result.items);
    if (page + 1 >= result.page.totalPages) break;
  }
  return items;
}

export type PartyNames = {
  /** Adı bilinmeyen (pasif ya da silinmiş) kimlik kısaltılarak gösterilir. */
  nameOf: (id: string) => string;
  /** Ada göre kimlik (büyük/küçük harf duyarsız); bilinmiyorsa undefined. */
  idOf: (name: string) => string | undefined;
};

/** Fasoncu adlarını tanım listelerinden çözer; liste alınamazsa kimlik kısaltması kalır. */
export function usePartyNames(): PartyNames {
  const query = useQuery({
    queryKey: ['/api/v1/master-data', 'report-names'],
    staleTime: NAMES_STALE_MS,
    queryFn: async () => {
      const subcontractors = await fetchAll((page) =>
        listSubcontractors({ page, size: PAGE_SIZE }),
      );
      return new Map<string, string>(
        subcontractors.map((row): [string, string] => [row.id, row.title]),
      );
    },
  });
  const names = query.data;
  return {
    nameOf: (id) => names?.get(id) ?? shortId(id),
    idOf: (name) => {
      const wanted = name.trim().toLocaleLowerCase('tr');
      for (const [id, title] of names ?? []) {
        if (title.trim().toLocaleLowerCase('tr') === wanted) return id;
      }
      return undefined;
    },
  };
}
