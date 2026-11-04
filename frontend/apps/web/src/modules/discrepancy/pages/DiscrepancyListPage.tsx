import { useTranslation } from '@iplik/i18n';
import { Stack, Text, Title } from '@iplik/ui';
import { ListPagination, QueryState } from '@/modules/shipment';
import { DiscrepancyCard } from '../components/DiscrepancyCard';
import { useDiscrepancies } from '../hooks/useDiscrepancies';
import { DiscrepancyFilters } from '../components/DiscrepancyFilters';
import { PAGE_SIZE, useDiscrepancyFilter } from '../hooks/useDiscrepancyFilter';

export function DiscrepancyListPage() {
  const { t } = useTranslation();
  const filter = useDiscrepancyFilter();
  const query = useDiscrepancies({
    page: filter.page - 1,
    size: PAGE_SIZE,
    ...(filter.status === undefined ? {} : { status: filter.status }),
    ...(filter.severity === undefined ? {} : { severity: filter.severity }),
    ...(filter.isOverdue ? { overdue: true } : {}),
  });
  return (
    <Stack>
      <Title order={1} size="h2">
        {t('discrepancy.list.title')}
      </Title>
      <DiscrepancyFilters filter={filter} />
      <QueryState query={query}>
        {(page) =>
          page.items.length === 0 ? (
            <Text c="dimmed">{t('discrepancy.list.empty')}</Text>
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
