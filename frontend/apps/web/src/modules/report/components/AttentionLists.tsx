import type { DashboardView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { formatDateTime } from '@iplik/i18n';
import { Anchor, Badge, Group, Paper, Stack, Text, Title } from '@iplik/ui';
import { Link } from 'react-router';
import { formatKilograms } from '../format';

/** Vadesi geçen uyuşmazlıklar ve kilo farkı en büyük sevkiyatlar (ön analiz Bölüm 13). */
export function AttentionLists({ dashboard }: { dashboard: DashboardView }) {
  const { t } = useTranslation();
  return (
    <Stack>
      <Title order={2} size="h3">
        {t('report.dashboard.overdueList')}
      </Title>
      {dashboard.overdueDiscrepancies.length === 0 ? (
        <Text c="dimmed">{t('report.dashboard.noOverdue')}</Text>
      ) : (
        dashboard.overdueDiscrepancies.map((item) => (
          <Paper key={item.discrepancyId} withBorder p="sm">
            <Group justify="space-between">
              <Anchor component={Link} to={`/panel/uyusmazliklar/${item.discrepancyId}`}>
                {item.discrepancyNo}
              </Anchor>
              <Group gap="xs">
                <Badge color={item.severity === 'RED' ? 'red' : 'yellow'}>
                  {t(`discrepancy.severity.${item.severity}`)}
                </Badge>
                <Text size="sm" c="dimmed">
                  {t('report.dashboard.dueAt', { at: formatDateTime(item.dueAt) })}
                </Text>
              </Group>
            </Group>
          </Paper>
        ))
      )}
      <Title order={2} size="h3">
        {t('report.dashboard.largestList')}
      </Title>
      {dashboard.largestDifferences.length === 0 ? (
        <Text c="dimmed">{t('report.dashboard.noDifferences')}</Text>
      ) : (
        dashboard.largestDifferences.map((item) => (
          <Paper key={item.shipmentId} withBorder p="sm">
            <Group justify="space-between">
              <Anchor component={Link} to={`/panel/sevkiyat/${item.shipmentId}`}>
                {item.shipmentNo}
              </Anchor>
              <Group gap="xs">
                <Badge color={bandColor(item.band)}>
                  {t(`report.dashboard.band.${item.band}`)}
                </Badge>
                <Text size="sm">{formatKilograms(item.diffKg)}</Text>
              </Group>
            </Group>
          </Paper>
        ))
      )}
    </Stack>
  );
}

function bandColor(band: 'GREEN' | 'YELLOW' | 'RED'): string {
  if (band === 'RED') return 'red';
  return band === 'YELLOW' ? 'yellow' : 'green';
}
