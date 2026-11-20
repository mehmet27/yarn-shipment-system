import { useTranslation } from '@iplik/i18n';
import { Group, Stack, Title } from '@iplik/ui';
import { useState } from 'react';
import { QueryState } from '@/modules/shared';
import { useDashboard } from '../hooks/useDashboard';
import type { PeriodDays } from '../period';
import { DEFAULT_PERIOD, toPeriodParams } from '../period';
import { AttentionLists } from './AttentionLists';
import { DashboardSummary } from './DashboardSummary';
import { StockOverview } from './StockOverview';
import { PeriodSelect } from './PeriodSelect';

/** Panel ana sayfasının dashboard bölümü: önce karar bekleyenler, sonra dönem özeti; rapor yetkisi gerekir. */
export function DashboardSection() {
  const { t } = useTranslation();
  const [days, setDays] = useState<PeriodDays>(DEFAULT_PERIOD);
  const query = useDashboard(toPeriodParams(days));
  return (
    <Stack>
      {query.data === undefined ? null : <AttentionLists dashboard={query.data} />}
      <Group justify="space-between">
        <Title order={2}>{t('report.dashboard.title')}</Title>
        <PeriodSelect value={days} onChange={setDays} />
      </Group>
      <QueryState query={query}>
        {(dashboard) => (
          <Stack>
            <StockOverview params={toPeriodParams(days)} />
            <DashboardSummary dashboard={dashboard} />
          </Stack>
        )}
      </QueryState>
    </Stack>
  );
}
