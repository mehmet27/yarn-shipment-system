import type { DashboardView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { SectionCard } from '@iplik/ui';
import { formatKilograms } from '../format';
import { CardBleed } from './CardBleed';
import { StatCard } from './StatCard';
import { ShipmentStatusBar } from './ShipmentStatusBar';
import { StatGrid } from './StatGrid';

/**
 * Dashboard kartları: sevkiyatların durum dağılımı (dilimli çubuk) ve kilo karşılaştırması tek kartta, açık
 * uyuşmazlıklar ayrı kartta.
 */
export function DashboardSummary({ dashboard }: { dashboard: DashboardView }) {
  const { t } = useTranslation();
  const { shipments, quantity, discrepancies } = dashboard;
  return (
    <>
      <SectionCard title={t('report.dashboard.shipments')} icon="shipments">
        <CardBleed>
          <ShipmentStatusBar shipments={shipments} />
          <StatGrid>
            <StatCard
              label={t('report.dashboard.total')}
              value={shipments.total}
              testId="stat-total"
            />
            <StatCard
              label={t('report.dashboard.dispatchNet')}
              value={formatKilograms(quantity.dispatchNetKg)}
            />
            <StatCard
              label={t('report.dashboard.deliveryNet')}
              value={formatKilograms(quantity.deliveryNetKg)}
            />
          </StatGrid>
        </CardBleed>
      </SectionCard>
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
    </>
  );
}
