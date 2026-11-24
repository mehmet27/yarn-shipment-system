import { useTranslation } from '@iplik/i18n';
import { EmptyState, PageHero, Stack } from '@iplik/ui';
import { ListExportButton } from '@/modules/report';
import { ListPagination, QueryState } from '@/modules/shared';
import { DiscrepancyCard } from '../components/DiscrepancyCard';
import { discrepancyListTable, fetchAllDiscrepancies } from '../components/discrepancyListTable';
import { useDiscrepancies } from '../hooks/useDiscrepancies';
import { DiscrepancyFilters } from '../components/DiscrepancyFilters';
import { PAGE_SIZE, useDiscrepancyFilter } from '../hooks/useDiscrepancyFilter';

export function DiscrepancyListPage() {
  const { t } = useTranslation();
  const filter = useDiscrepancyFilter();
  const params = {
    ...(filter.status === undefined ? {} : { status: filter.status }),
    ...(filter.severity === undefined ? {} : { severity: filter.severity }),
    ...(filter.isOverdue ? { due: 'OVERDUE' as const } : {}),
  };
  const query = useDiscrepancies({ page: filter.page - 1, size: PAGE_SIZE, ...params });
  return (
    <Stack>
      <PageHero
        title={t('discrepancy.list.title')}
        actions={
          <ListExportButton
            fileName="uyusmazliklar.xlsx"
            loadTable={async () => discrepancyListTable(await fetchAllDiscrepancies(params), t)}
          />
        }
      />
      <DiscrepancyFilters filter={filter} />
      <QueryState query={query}>
        {(page) =>
          page.items.length === 0 ? (
            <EmptyState title={t('discrepancy.list.empty')} />
          ) : (
            <Stack>
              {page.items.map((item) => (
                <DiscrepancyCard key={item.id} discrepancy={item} />
              ))}
              <ListPagination
                total={page.page.totalPages}
                value={filter.page}
                onChange={filter.setPage}
              />
            </Stack>
          )
        }
      </QueryState>
    </Stack>
  );
}
