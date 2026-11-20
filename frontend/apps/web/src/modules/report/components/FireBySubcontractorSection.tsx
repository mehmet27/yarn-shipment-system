import type { GetDashboardParams } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { SectionCard, Text } from '@iplik/ui';
import { QueryState } from '@/modules/shipment';
import { useYarnVsFabricReport } from '../hooks/useFaconReports';
import { FireBars } from './FireBars';

/** "Kimde ne kadar fire var, sınırı aşan var mı?": seçilen dönemde fasoncu başına gerçekleşen fire. */
export function FireBySubcontractorSection({ params }: { params: GetDashboardParams }) {
  const { t } = useTranslation();
  const query = useYarnVsFabricReport(params, true);
  return (
    <SectionCard
      title={t('report.charts.fire.title')}
      description={t('report.charts.fire.description')}
      icon="discrepancies"
      testId="section-fire"
    >
      <QueryState query={query}>
        {(rows) =>
          rows.length === 0 ? (
            <Text c="dimmed">{t('report.fire.empty')}</Text>
          ) : (
            <FireBars rows={rows} testId="chart-fire" />
          )
        }
      </QueryState>
    </SectionCard>
  );
}
