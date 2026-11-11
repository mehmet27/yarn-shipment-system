import type { DashboardView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { formatDateTime } from '@iplik/i18n';
import { Anchor, Badge, Group, Paper, Stack, Text, Title } from '@iplik/ui';
import { Link } from 'react-router';

/** Vadesi geçen uyuşmazlıklar (ön analiz Bölüm 13). */
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
    </Stack>
  );
}
