import type { DiscrepancyView } from '@iplik/api-client';
import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Group, Paper, Stack, Text } from '@iplik/ui';
import { Link } from 'react-router';
import { DiscrepancyBadges } from './DiscrepancyBadges';
import { toSeverityColor } from './discrepancyColor';
import { summaryOf } from './discrepancyText';

/** Dar ekranda liste satırı: tek dokunuşla açılan büyük kart, solda önem rengi. */
export function DiscrepancyCard({ discrepancy }: { discrepancy: DiscrepancyView }) {
  const { t } = useTranslation();
  return (
    <Paper
      component={Link}
      to={`/panel/uyusmazliklar/${discrepancy.id}`}
      withBorder
      p="md"
      mih={72}
      c="dark"
      aria-label={discrepancy.discrepancyNo}
      style={{
        textDecoration: 'none',
        borderLeft: `4px solid var(--mantine-color-${toSeverityColor(discrepancy.severity)}-5)`,
      }}
    >
      <Stack gap={4}>
        <Group justify="space-between" wrap="nowrap">
          <Text fw={700} size="lg">
            {discrepancy.discrepancyNo}
          </Text>
          <DiscrepancyBadges discrepancy={discrepancy} />
        </Group>
        <Text fw={500}>{t(`discrepancy.type.${discrepancy.type}`)}</Text>
        <Text size="sm" c="dimmed">
          {[
            summaryOf(t, discrepancy),
            discrepancy.dueAt === undefined
              ? undefined
              : `${t('discrepancy.due')}: ${formatDateTime(discrepancy.dueAt)}`,
          ]
            .filter((part) => part !== undefined && part !== '')
            .join(' · ')}
        </Text>
      </Stack>
    </Paper>
  );
}
