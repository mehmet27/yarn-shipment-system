import { DiscrepancyStatus, Severity } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Group, Pagination, Select, Stack, Text, Title } from '@iplik/ui';
import { QueryState } from '@/modules/shipment';
import { DiscrepancyCard } from '../components/DiscrepancyCard';
import { useDiscrepancies } from '../hooks/useDiscrepancies';
import { PAGE_SIZE, useDiscrepancyFilter } from '../hooks/useDiscrepancyFilter';

const STATUSES = Object.values(DiscrepancyStatus);
const SEVERITIES = Object.values(Severity);

export function DiscrepancyListPage() {
  const { t } = useTranslation();
  const filter = useDiscrepancyFilter();
  const query = useDiscrepancies({
    page: filter.page - 1,
    size: PAGE_SIZE,
    ...(filter.status === undefined ? {} : { status: filter.status }),
    ...(filter.severity === undefined ? {} : { severity: filter.severity }),
  });
  return (
    <Stack>
      <Title order={1} size="h2">
        {t('discrepancy.list.title')}
      </Title>
      <Group grow align="flex-end">
        <Select
          label={t('discrepancy.list.statusFilter')}
          placeholder={t('discrepancy.list.all')}
          data={STATUSES.map((value) => ({ value, label: t(`discrepancy.status.${value}`) }))}
          value={filter.status ?? null}
          onChange={(value) => {
            filter.setStatus(STATUSES.find((item) => item === value));
          }}
          clearable
        />
        <Select
          label={t('discrepancy.list.severityFilter')}
          placeholder={t('discrepancy.list.all')}
          data={SEVERITIES.map((value) => ({ value, label: t(`discrepancy.severity.${value}`) }))}
          value={filter.severity ?? null}
          onChange={(value) => {
            filter.setSeverity(SEVERITIES.find((item) => item === value));
          }}
          clearable
        />
      </Group>
      <QueryState query={query}>
        {(page) =>
          page.items.length === 0 ? (
            <Text c="dimmed">{t('discrepancy.list.empty')}</Text>
          ) : (
            <Stack>
              {page.items.map((item) => (
                <DiscrepancyCard key={item.id} discrepancy={item} />
              ))}
              <Pagination
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
