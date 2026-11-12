import type { GetDashboardParams } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { SimpleGrid, Stack, Title } from '@iplik/ui';
import { usePermission } from '@/modules/auth';
import { formatKilograms } from '../format';
import { useYarnVsFabricReport } from '../hooks/useFaconReports';
import { useCentralStockTotals, useSubcontractorYarn } from '../hooks/useStockOverview';
import { StatCard } from './StatCard';
import { sumFabricKg, sumInTransitKg } from './stockTotals';

const CARD_COLUMNS = { base: 1, sm: 2, lg: 4 } as const;
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
    <Stack>
      <Title order={2} size="h3">
        {t('report.dashboard.stock')}
      </Title>
      <SimpleGrid cols={CARD_COLUMNS}>
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
      </SimpleGrid>
    </Stack>
  );
}
