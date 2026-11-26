import { listProducts } from '@iplik/api-client';
import { useQuery } from '@tanstack/react-query';

const PAGE_SIZE = 100;
const normalize = (name: string) => name.trim().toLocaleLowerCase('tr');

async function activeYarnNames(): Promise<Set<string>> {
  const names = new Set<string>();
  let page = 0;
  let totalPages = 1;
  while (page < totalPages) {
    const result = await listProducts({ page, size: PAGE_SIZE });
    for (const row of result.items) names.add(normalize(row.name));
    totalPages = result.page.totalPages;
    page += 1;
  }
  return names;
}

/** Tanımlar'daki etkin iplik adı mı (harf büyüklüğü ve kenar boşlukları sayılmaz). */
export function useKnownYarns() {
  const query = useQuery({
    queryKey: ['/api/v1/master-data', 'product', 'opening-names'],
    queryFn: activeYarnNames,
  });
  return (name: string) => query.data?.has(normalize(name)) ?? false;
}
