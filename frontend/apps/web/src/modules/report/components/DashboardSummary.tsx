import type { DashboardView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { SimpleGrid, Stack, Title } from '@iplik/ui';
import { formatKilograms } from '../format';
import { StatCard } from './StatCard';

const CARD_COLUMNS = { base: 1, sm: 2, lg: 4 } as const;

/** Dashboard kartları: sevkiyat durumu, kilo, bantlar ve açık uyuşmazlıklar. */
export function DashboardSummary({ dashboard }: { dashboard: DashboardView }) {
  const { t } = useTranslation();
  const { shipments, quantity, bands, discrepancies } = dashboard;
  return (
    <Stack>
      <Title order={2} size="h3">
        {t('report.dashboard.shipments')}
      </Title>
      <SimpleGrid cols={CARD_COLUMNS}>
        <StatCard label={t('report.dashboard.total')} value={shipments.total} testId="stat-total" />
        <StatCard label={t('report.dashboard.inProgress')} value={shipments.inProgress} />
        <StatCard label={t('report.dashboard.inReview')} value={shipments.inReview} />
        <StatCard label={t('report.dashboard.completed')} value={shipments.completed} />
      </SimpleGrid>
      <Title order={2} size="h3">
        {t('report.dashboard.comparison')}
      </Title>
      <SimpleGrid cols={CARD_COLUMNS}>
        <StatCard
          label={t('report.dashboard.compared')}
          value={quantity.comparedShipments}
          hint={t('report.dashboard.comparedHint')}
        />
        <StatCard label={t('report.dashboard.band.GREEN')} value={bands.green} color="green" />
        <StatCard label={t('report.dashboard.band.YELLOW')} value={bands.yellow} color="yellow.8" />
        <StatCard label={t('report.dashboard.band.RED')} value={bands.red} color="red" />
        <StatCard
          label={t('report.dashboard.dispatchNet')}
          value={formatKilograms(quantity.dispatchNetKg)}
        />
        <StatCard
          label={t('report.dashboard.deliveryNet')}
          value={formatKilograms(quantity.deliveryNetKg)}
        />
        <StatCard
          label={t('report.dashboard.absoluteDiff')}
          value={formatKilograms(quantity.absoluteDiffKg)}
        />
      </SimpleGrid>
      <Title order={2} size="h3">
        {t('report.dashboard.discrepancies')}
      </Title>
      <SimpleGrid cols={CARD_COLUMNS}>
        <StatCard
          label={t('report.dashboard.open')}
          value={discrepancies.open}
          testId="stat-open"
        />
        <StatCard label={t('report.dashboard.openRed')} value={discrepancies.red} color="red" />
        <StatCard label={t('report.dashboard.openYellow')} value={discrepancies.yellow} />
        <StatCard
          label={t('report.dashboard.overdue')}
          value={discrepancies.overdue}
          color={discrepancies.overdue > 0 ? 'red' : undefined}
          testId="stat-overdue"
        />
      </SimpleGrid>
    </Stack>
  );
}
