import { DiscrepancyStatus, Severity } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Checkbox, Group, Select, Stack } from '@iplik/ui';
import type { DiscrepancyFilter } from '../hooks/useDiscrepancyFilter';

const STATUSES = Object.values(DiscrepancyStatus);
const SEVERITIES = Object.values(Severity);

/** Uyuşmazlık listesinin durum, önem ve "süresi geçenler" süzgeçleri. */
export function DiscrepancyFilters({ filter }: { filter: DiscrepancyFilter }) {
  const { t } = useTranslation();
  return (
    <Stack>
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
      <Checkbox
        label={t('discrepancy.list.overdueFilter')}
        checked={filter.isOverdue}
        onChange={(event) => {
          filter.setOverdue(event.currentTarget.checked);
        }}
      />
    </Stack>
  );
}
