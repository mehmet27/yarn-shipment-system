import { useTranslation } from '@iplik/i18n';
import { Paper } from '@iplik/ui';
import { sumKg } from '@iplik/units';
import { StatCell, StatGrid } from '@/modules/report';
import { formatKgText, formatPercentText, formatSignedKg } from '@/modules/shipment';
import type { SubcontractorOverview } from '../hooks/useSubcontractorOverview';

const PERCENT = 100;
const RATE_DECIMALS = 2;

/** Sunucu oranı kesirdir (`0.0403`); ekranda `%4,03` yazılır. */
function fireRateText(rate: string): string {
  return formatPercentText((Number(rate) * PERCENT).toFixed(RATE_DECIMALS));
}

/** Fasoncunun tek bakışta durumu: elindeki iplik, yoldaki sevkiyat, beklenen kumaş, faturalanacak kilo, fire. */
export function SubcontractorStats({ overview }: { overview: SubcontractorOverview }) {
  const { t } = useTranslation();
  const { balances, inTransit, awaitingFabric, toInvoice, fireRate, openDiscrepancies } = overview;
  return (
    <Paper withBorder style={{ overflow: 'hidden' }}>
      <StatGrid>
        <StatCell
          label={t('subcontractorPage.stat.balance')}
          value={balances.data === undefined ? '—' : formatSignedKg(balances.data.totalKg)}
          testId="sub-balance"
        />
        <StatCell
          label={t('subcontractorPage.stat.inTransit')}
          value={inTransit.length}
          testId="sub-in-transit"
        />
        <StatCell
          label={t('subcontractorPage.stat.awaitingFabric')}
          value={formatKgText(sumKg(awaitingFabric.map((row) => row.expectedFabricKg)))}
          hint={t('subcontractorPage.stat.files', { count: awaitingFabric.length })}
          testId="sub-awaiting-fabric"
        />
        <StatCell
          label={t('subcontractorPage.stat.toInvoice')}
          value={formatKgText(sumKg(toInvoice.map((row) => row.remainingKg)))}
          hint={t('subcontractorPage.stat.files', { count: toInvoice.length })}
          testId="sub-to-invoice"
        />
        {fireRate === undefined ? null : (
          <StatCell
            label={t('subcontractorPage.stat.fireRate')}
            value={fireRateText(fireRate)}
            testId="sub-fire-rate"
          />
        )}
        {overview.canSeeDiscrepancies && (
          <StatCell
            label={t('subcontractorPage.stat.openDiscrepancies')}
            value={openDiscrepancies.length}
            color={openDiscrepancies.length > 0 ? 'red' : undefined}
            testId="sub-open-discrepancies"
          />
        )}
      </StatGrid>
    </Paper>
  );
}
