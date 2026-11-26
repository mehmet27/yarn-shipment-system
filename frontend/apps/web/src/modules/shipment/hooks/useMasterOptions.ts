import {
  listDrivers,
  listLots,
  listProducts,
  listSubcontractors,
  listQualityGrades,
  listVehicles,
  listYarnSuppliers,
} from '@iplik/api-client';
import { useQuery } from '@tanstack/react-query';

export const MASTER_DATA_PATH = '/api/v1/master-data';
const OPTION_PAGE_SIZE = 20;
/** Partiler ipliğe göre ekranda süzülür; süzmeden önce daha geniş bir sayfa istenir. */
const LOT_FILTER_PAGE_SIZE = 100;
/** Merkez şoförlerinin grup başlığı anahtarı; seçici bunu çevirip gösterir. */
export const CENTRAL_GROUP = '__central__';

export type MasterKind =
  'subcontractor' | 'driver' | 'vehicle' | 'product' | 'lot' | 'supplier' | 'qualityGrade';
/** `group` doluysa seçici seçenekleri bu başlık altında gruplar (şoförde "Merkez" ve fasoncu). */
export type MasterOption = { value: string; label: string; group?: string };

type Search = { q?: string; size: number };
/** Listeyi daraltan seçim: şoförde fasoncu, partide iplik. */
export type MasterScope = { subcontractorId?: string | undefined; productId?: string | undefined };

/** İplik verildiyse yalnızca o ipliğin partileri. */
async function lotOptions(search: Search, productId: string | undefined): Promise<MasterOption[]> {
  const query = productId === undefined ? search : { ...search, size: LOT_FILTER_PAGE_SIZE };
  return (await listLots(query)).items
    .filter((row) => productId === undefined || row.productId === productId)
    .map((row) => ({ value: row.id, label: row.lotNo }));
}

async function fetchOptions(
  kind: MasterKind,
  search: Search,
  { subcontractorId, productId }: MasterScope,
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
      ).items.map((row) => ({
        value: row.id,
        label: row.fullName,
        // Fasoncu seçiliyken liste merkez şoförlerini de içerir; hangisinin nereden geldiği başlıkla ayrılır.
        ...(subcontractorId === undefined
          ? {}
          : {
              group:
                row.subcontractorId === undefined ? CENTRAL_GROUP : (row.subcontractorTitle ?? ''),
            }),
      }));
    case 'vehicle':
      return (await listVehicles(search)).items.map((row) => ({
        value: row.id,
        label: row.plateNo,
      }));
    case 'product':
      return (await listProducts(search)).items.map((row) => ({ value: row.id, label: row.name }));
    case 'lot':
      return lotOptions(search, productId);
    case 'supplier':
      return (await listYarnSuppliers(search)).items.map((row) => ({
        value: row.id,
        label: row.name,
      }));
    case 'qualityGrade':
      return (await listQualityGrades(search)).items.map((row) => ({
        value: row.id,
        label: `${row.code} · ${row.name}`,
      }));
  }
}

/** Seçici listeleri sunucuda aranır (`q`); mobilde tüm listeyi indirmek yerine ilk sayfa gelir. */
export function useMasterOptions(kind: MasterKind, q: string, scope: MasterScope = {}) {
  return useQuery({
    queryKey: [MASTER_DATA_PATH, kind, q, scope.subcontractorId, scope.productId],
    queryFn: () =>
      fetchOptions(kind, { size: OPTION_PAGE_SIZE, ...(q === '' ? {} : { q }) }, scope),
    staleTime: Infinity,
  });
}
