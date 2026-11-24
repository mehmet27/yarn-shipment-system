import type { DiscrepancyView, ListDiscrepanciesParams } from '@iplik/api-client';
import { listDiscrepancies } from '@iplik/api-client';
import { formatDateTime } from '@iplik/i18n';
import type { ReportTable } from '@/modules/report';
import { kg, text } from '@/modules/report';

const EXPORT_PAGE_SIZE = 100;
/** En çok 5.000 uyuşmazlık aktarılır; fazlası için süzgeç daraltılır. */
const MAX_EXPORT_PAGES = 50;

type Translate = (key: string) => string;

/** Süzgece uyan tüm uyuşmazlıklar (Excel için); sayfa sayfa okunur. */
export async function fetchAllDiscrepancies(
  params: Omit<ListDiscrepanciesParams, 'page' | 'size'>,
): Promise<DiscrepancyView[]> {
  const rows: DiscrepancyView[] = [];
  for (let page = 0; page < MAX_EXPORT_PAGES; page += 1) {
    const result = await listDiscrepancies({ ...params, page, size: EXPORT_PAGE_SIZE });
    rows.push(...result.items);
    if (page + 1 >= result.page.totalPages) break;
  }
  return rows;
}

const optionalDateTime = (value: string | undefined) =>
  text(value === undefined ? '' : formatDateTime(value));

/** Uyuşmazlık listesinin Excel sayfası. */
export function discrepancyListTable(
  discrepancies: readonly DiscrepancyView[],
  t: Translate,
): ReportTable {
  return {
    title: t('discrepancy.list.title'),
    headers: [
      t('discrepancy.export.no'),
      t('discrepancy.facts.type'),
      t('discrepancy.list.severityFilter'),
      t('discrepancy.list.statusFilter'),
      t('discrepancy.export.differenceKg'),
      t('discrepancy.facts.due'),
      t('discrepancy.facts.resolution'),
      t('discrepancy.export.createdAt'),
    ],
    rows: discrepancies.map((item) => [
      text(item.discrepancyNo),
      text(t(`discrepancy.type.${item.type}`)),
      text(t(`discrepancy.severity.${item.severity}`)),
      text(t(`discrepancy.status.${item.status}`)),
      item.diffKg === undefined ? text('') : kg(item.diffKg),
      optionalDateTime(item.dueAt),
      text(item.resolution === undefined ? '' : t(`discrepancy.resolution.${item.resolution}`)),
      optionalDateTime(item.createdAt),
    ]),
  };
}
