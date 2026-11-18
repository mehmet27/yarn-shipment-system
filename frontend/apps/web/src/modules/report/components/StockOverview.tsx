import type { GetDashboardParams } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { SectionCard } from '@iplik/ui';
import { usePermission } from '@/modules/auth';
import { formatKilograms } from '../format';
import { useYarnVsFabricReport } from '../hooks/useFaconReports';
import { useCentralStockTotals, useSubcontractorYarn } from '../hooks/useStockOverview';
import { AmountOverview } from './AmountOverview';
import { CardBleed } from './CardBleed';
import { StatCard } from './StatCard';
import { StatGrid } from './StatGrid';
import { sumFabricKg, sumInTransitKg } from './stockTotals';

const NO_VALUE = '—';

function kgOrDash(value: string | undefined): string {
  return value === undefined ? NO_VALUE : formatKilograms(value);
}

/** Dört stok göstergesi: merkez depoda iplik, fasoncularda iplik, fasoncudan gelen kumaş, merkezde üretilen kumaş. */
export function StockOverview({ params }: { params: GetDashboardParams }) {
  const { t } = useTranslation();
  const central = useCentralStockTotals(usePermission('CENTRAL_STOCK_VIEW')).data;
  const subcontractors = useSubcontractorYarn(usePermission('BALANCE_VIEW')).data;
  const fabric = useYarnVsFabricReport(params, true).data;
  const inTransit = sumInTransitKg(subcontractors);
  const isCentralNegative = central?.totalKg.startsWith('-') ?? false;
  return (
    <>
      <SectionCard title={t('report.dashboard.stock')} icon="balances">
        <CardBleed>
          <StatGrid>
            <StatCard
              label={t('report.dashboard.centralYarn')}
              value={kgOrDash(central?.totalKg)}
              color={isCentralNegative ? 'red' : undefined}
              hint={isCentralNegative ? t('report.dashboard.negativeStock') : undefined}
              testId="stat-central-yarn"
            />
            <StatCard
              label={t('report.dashboard.subcontractorYarn')}
              value={kgOrDash(subcontractors?.totalKg)}
              hint={t('report.dashboard.inTransit', { kg: kgOrDash(inTransit) })}
              testId="stat-subcontractor-yarn"
            />
            <StatCard
              label={t('report.dashboard.fabricReceived')}
              value={kgOrDash(sumFabricKg(fabric))}
              hint={t('report.dashboard.selectedPeriod')}
              testId="stat-fabric-received"
            />
            <StatCard
              label={t('report.dashboard.centralFabric')}
              value={kgOrDash(central?.producedFabricKg)}
              hint={t('report.dashboard.allTime')}
              testId="stat-central-fabric"
            />
          </StatGrid>
        </CardBleed>
      </SectionCard>
      <AmountOverview params={params} />
    </>
  );
}
