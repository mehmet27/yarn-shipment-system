import type { DashboardView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { SectionCard } from '@iplik/ui';
import { formatKilograms } from '../format';
import { usePeriodHint } from '../hooks/usePeriodHint';
import { CardBleed } from './CardBleed';
import { OpenDiscrepanciesCard } from './OpenDiscrepanciesCard';
import { StatCard } from './StatCard';
import { ShipmentStatusBar } from './ShipmentStatusBar';
import { StatGrid } from './StatGrid';

export type DashboardSummaryProps = {
  dashboard: DashboardView;
  previous?: DashboardView | undefined;
};

/**
 * Dashboard kartları: sevkiyatların durum dağılımı (dilimli çubuk) ve kilo karşılaştırması tek kartta, açık
 * uyuşmazlıklar ayrı kartta. `previous` verilirse döneme bağlı sayıların altında önceki dönem kıyası yazar; açık
 * uyuşmazlıklar bugünkü durumdur, kıyaslanmaz.
 */
export function DashboardSummary({ dashboard, previous }: DashboardSummaryProps) {
  const { t } = useTranslation();
  const toHint = usePeriodHint();
  const { shipments, quantity, discrepancies } = dashboard;
  const kgHint = (current: string, before: string | undefined) =>
    toHint(
      Number(current),
      before === undefined ? undefined : Number(before),
      formatKilograms(before ?? ''),
    );
  return (
    <>
      <SectionCard title={t('report.dashboard.shipments')} icon="shipments">
        <CardBleed>
          <ShipmentStatusBar shipments={shipments} />
          <StatGrid>
            <StatCard
              label={t('report.dashboard.total')}
              value={shipments.total}
              hint={toHint(
                shipments.total,
                previous?.shipments.total,
                String(previous?.shipments.total),
              )}
              testId="stat-total"
            />
            <StatCard
              label={t('report.dashboard.dispatchNet')}
              value={formatKilograms(quantity.dispatchNetKg)}
              hint={kgHint(quantity.dispatchNetKg, previous?.quantity.dispatchNetKg)}
            />
            <StatCard
              label={t('report.dashboard.deliveryNet')}
              value={formatKilograms(quantity.deliveryNetKg)}
              hint={kgHint(quantity.deliveryNetKg, previous?.quantity.deliveryNetKg)}
            />
          </StatGrid>
        </CardBleed>
      </SectionCard>
      <OpenDiscrepanciesCard discrepancies={discrepancies} />
    </>
  );
}
