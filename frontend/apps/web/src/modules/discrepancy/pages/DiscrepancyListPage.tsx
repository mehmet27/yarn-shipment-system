import { useTranslation } from '@iplik/i18n';
import type { DiscrepancyView } from '@iplik/api-client';
import { PageHero, Stack, Text } from '@iplik/ui';
import { ListPagination, QueryState } from '@/modules/shipment';
import { DiscrepancyCard } from '../components/DiscrepancyCard';
import { useDiscrepancies } from '../hooks/useDiscrepancies';
import { DiscrepancyFilters } from '../components/DiscrepancyFilters';
import { PAGE_SIZE, useDiscrepancyFilter } from '../hooks/useDiscrepancyFilter';

/** Aynı sevkiyat ve kuralda özdeş görünen kayıtlara sıra numarası verir (ayırt edilebilsinler). */
function sequences(items: DiscrepancyView[]): Map<string, { index: number; total: number }> {
  const groups = new Map<string, string[]>();
  for (const item of items) {
    const key = `${item.shipmentId}|${item.ruleCode}|${item.expectedValue ?? ''}|${item.actualValue ?? ''}`;
    groups.set(key, [...(groups.get(key) ?? []), item.id]);
  }
  const result = new Map<string, { index: number; total: number }>();
  for (const ids of groups.values()) {
    if (ids.length < 2) continue;
    ids.forEach((id, i) => result.set(id, { index: i + 1, total: ids.length }));
  }
  return result;
}

export function DiscrepancyListPage() {
  const { t } = useTranslation();
  const filter = useDiscrepancyFilter();
  const query = useDiscrepancies({
    page: filter.page - 1,
    size: PAGE_SIZE,
    ...(filter.status === undefined ? {} : { status: filter.status }),
    ...(filter.severity === undefined ? {} : { severity: filter.severity }),
    ...(filter.isOverdue ? { due: 'OVERDUE' as const } : {}),
  });
  return (
    <Stack>
      <PageHero title={t('discrepancy.list.title')} />
      <DiscrepancyFilters filter={filter} />
      <QueryState query={query}>
        {(page) =>
          page.items.length === 0 ? (
            <Text c="dimmed">{t('discrepancy.list.empty')}</Text>
          ) : (
            <Stack>
              {page.items.map((item, _i, all) => (
                <DiscrepancyCard
                  key={item.id}
                  discrepancy={item}
                  sequence={sequences(all).get(item.id)}
                />
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
