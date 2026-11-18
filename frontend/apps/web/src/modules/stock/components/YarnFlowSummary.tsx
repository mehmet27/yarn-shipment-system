import { useTranslation } from '@iplik/i18n';
import type { CentralStock } from '@iplik/api-client/central-stock';
import { Box, SectionCard } from '@iplik/ui';
import { StatCell, StatGrid } from '@/modules/report';
import { formatKgText, formatSignedKg } from '@/modules/shipment';

/** Gelen ipliğin nerede olduğu: merkez depoda, fasoncularda, merkez üretiminde; kapıdan aktarılan ayrıca gösterilir. */
export function YarnFlowSummary({ stock }: { stock: CentralStock }) {
  const { t } = useTranslation();
  const { flow } = stock;
  return (
    <SectionCard title={t('stock.summary.flowTitle')}>
      <Box style={{ margin: 'calc(var(--mantine-spacing-md) * -1)' }}>
        <StatGrid>
          <StatCell
            label={t('stock.summary.received')}
            value={formatKgText(flow.receivedKg)}
            hint={t('stock.summary.receivedHint')}
          />
          <StatCell label={t('stock.summary.atCentral')} value={formatSignedKg(stock.totalKg)} />
          <StatCell
            label={t('stock.summary.atSubcontractors')}
            value={formatKgText(flow.atSubcontractorsKg)}
            hint={t('stock.summary.atSubcontractorsHint')}
          />
          <StatCell
            label={t('stock.summary.inProduction')}
            value={formatKgText(flow.usedInCentralProductionKg)}
          />
          <StatCell
            label={t('stock.summary.direct')}
            value={formatKgText(flow.directTransferKg)}
            hint={t('stock.summary.directHint')}
          />
        </StatGrid>
      </Box>
    </SectionCard>
  );
}
