import { useTranslation } from '@iplik/i18n';
import { Group, Stack, Title } from '@iplik/ui';
import { useState } from 'react';
import { QueryState } from '@/modules/shipment';
import { useDashboard } from '../hooks/useDashboard';
import type { PeriodDays } from '../period';
import { DEFAULT_PERIOD, toPeriodParams } from '../period';
import { AttentionLists } from './AttentionLists';
import { DashboardSummary } from './DashboardSummary';
import { StockOverview } from './StockOverview';
import { PeriodSelect } from './PeriodSelect';

/** Panel ana sayfasının dashboard bölümü; yalnızca rapor yetkisi olan kullanıcıya gösterilir. */
export function DashboardSection() {
  const { t } = useTranslation();
  const [days, setDays] = useState<PeriodDays>(DEFAULT_PERIOD);
  const query = useDashboard(toPeriodParams(days));
  return (
    <Stack>
      <Group justify="space-between">
        <Title order={2} size="h3">
          {t('report.dashboard.title')}
        </Title>
        <PeriodSelect value={days} onChange={setDays} />
      </Group>
      <QueryState query={query}>
        {(dashboard) => (
          <Stack>
            <StockOverview params={toPeriodParams(days)} />
            <DashboardSummary dashboard={dashboard} />
            <AttentionLists dashboard={dashboard} />
          </Stack>
        )}
      </QueryState>
    </Stack>
  );
}
