import type { DashboardView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { SectionCard } from '@iplik/ui';
import { CardBleed } from './CardBleed';
import { StatCard } from './StatCard';
import { StatGrid } from './StatGrid';

/** Açık uyuşmazlıklar kartı: bugünkü durumdur, döneme bağlı değildir; bu yüzden önceki dönemle kıyaslanmaz. */
export function OpenDiscrepanciesCard({
  discrepancies,
}: {
  discrepancies: DashboardView['discrepancies'];
}) {
  const { t } = useTranslation();
  return (
    <SectionCard
      title={t('report.dashboard.discrepancies')}
      icon="discrepancies"
      tone={discrepancies.overdue > 0 ? 'danger' : 'brand'}
    >
      <CardBleed>
        <StatGrid>
          <StatCard
            label={t('report.dashboard.open')}
            value={discrepancies.open}
            testId="stat-open"
          />
          <StatCard
            label={t('report.dashboard.openRed')}
            value={discrepancies.red}
            color={discrepancies.red > 0 ? 'red' : undefined}
          />
          <StatCard label={t('report.dashboard.openYellow')} value={discrepancies.yellow} />
          <StatCard
            label={t('report.dashboard.overdue')}
            value={discrepancies.overdue}
            color={discrepancies.overdue > 0 ? 'red' : undefined}
            testId="stat-overdue"
          />
        </StatGrid>
      </CardBleed>
    </SectionCard>
  );
}
