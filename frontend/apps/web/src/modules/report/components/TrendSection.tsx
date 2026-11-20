import type { GetDashboardParams } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Loader, SectionCard, Stack, Text } from '@iplik/ui';
import { lazy, Suspense } from 'react';
import { QueryState } from '@/modules/shared';
import { isTrendEmpty } from '../charts/trendRows';
import { useMonthlyTrend } from '../hooks/useMonthlyTrend';
import { TrendTable } from './TrendTable';

const TrendChart = lazy(async () => ({ default: (await import('./TrendChart')).TrendChart }));

/** "Bu ay ne kadar iplik çıktı, ne kadar kumaş geldi, önceki aylara göre nasıl?" sorusunun cevabı. */
export function TrendSection({ params }: { params: GetDashboardParams }) {
  const { t } = useTranslation();
  const query = useMonthlyTrend(params);
  return (
    <SectionCard
      title={t('report.charts.trend.title')}
      description={t('report.charts.trend.description')}
      icon="reports"
      testId="section-trend"
    >
      <QueryState query={query}>
        {(flows) =>
          isTrendEmpty(flows) ? (
            <Text c="dimmed">{t('report.charts.trend.empty')}</Text>
          ) : (
            <Stack>
              <Suspense fallback={<Loader />}>
                <TrendChart flows={flows} />
              </Suspense>
              <TrendTable flows={flows} />
            </Stack>
          )
        }
      </QueryState>
    </SectionCard>
  );
}
