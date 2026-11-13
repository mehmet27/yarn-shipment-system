import { useTranslation } from '@iplik/i18n';
import type { CentralStock } from '@iplik/api-client/central-stock';
import { SimpleGrid, StatCard, Stack, Title } from '@iplik/ui';
import { formatKgText, formatSignedKg } from '@/modules/shipment';

/** Gelen ipliğin nerede olduğu: merkez depoda, fasoncularda, merkez üretiminde; kapıdan aktarılan ayrıca gösterilir. */
export function YarnFlowSummary({ stock }: { stock: CentralStock }) {
  const { t } = useTranslation();
  const { flow } = stock;
  return (
    <Stack gap="xs">
      <Title order={2} size="h3">
        {t('stock.summary.flowTitle')}
      </Title>
      <SimpleGrid cols={{ base: 1, sm: 2, md: 5 }}>
        <StatCard
          label={t('stock.summary.received')}
          value={formatKgText(flow.receivedKg)}
          hint={t('stock.summary.receivedHint')}
        />
        <StatCard label={t('stock.summary.atCentral')} value={formatSignedKg(stock.totalKg)} />
        <StatCard
          label={t('stock.summary.atSubcontractors')}
          value={formatKgText(flow.atSubcontractorsKg)}
          hint={t('stock.summary.atSubcontractorsHint')}
        />
        <StatCard
          label={t('stock.summary.inProduction')}
          value={formatKgText(flow.usedInCentralProductionKg)}
        />
        <StatCard
          label={t('stock.summary.direct')}
          value={formatKgText(flow.directTransferKg)}
          hint={t('stock.summary.directHint')}
        />
      </SimpleGrid>
    </Stack>
  );
}
