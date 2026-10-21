import { listDrivers, listSites, listSubcontractors, listVehicles } from '@iplik/api-client';
import { useQuery } from '@tanstack/react-query';

export const MASTER_DATA_PATH = '/api/v1/master-data';
const OPTION_PAGE_SIZE = 20;

export type MasterKind = 'subcontractor' | 'driver' | 'vehicle' | 'site';
export type MasterOption = { value: string; label: string };

type Search = { q?: string; size: number };

async function fetchOptions(
  kind: MasterKind,
  search: Search,
  subcontractorId: string | undefined,
): Promise<MasterOption[]> {
  switch (kind) {
    case 'subcontractor':
      return (await listSubcontractors(search)).items.map((row) => ({
        value: row.id,
        label: row.title,
      }));
    case 'driver':
      return (
        await listDrivers({
          ...search,
          ...(subcontractorId === undefined ? {} : { subcontractorId }),
        })
      ).items.map((row) => ({ value: row.id, label: row.fullName }));
    case 'vehicle':
      return (await listVehicles(search)).items.map((row) => ({
        value: row.id,
        label: row.plateNo,
      }));
    case 'site':
      return (await listSites(search)).items.map((row) => ({ value: row.id, label: row.name }));
  }
}

/** Seçici listeleri sunucuda aranır (`q`); mobilde tüm listeyi indirmek yerine ilk sayfa gelir. */
export function useMasterOptions(kind: MasterKind, q: string, subcontractorId?: string) {
  return useQuery({
    queryKey: [MASTER_DATA_PATH, kind, q, subcontractorId],
    queryFn: () =>
      fetchOptions(kind, { size: OPTION_PAGE_SIZE, ...(q === '' ? {} : { q }) }, subcontractorId),
    staleTime: Infinity,
  });
}
