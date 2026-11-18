import type { DiscrepancyView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Group } from '@iplik/ui';
import { DotBadge } from '@iplik/ui';
import { toDiscrepancyStatusColor, toSeverityColor } from './discrepancyColor';

export function DiscrepancyBadges({ discrepancy }: { discrepancy: DiscrepancyView }) {
  const { t } = useTranslation();
  return (
    <Group gap="xs">
      <DotBadge color={toSeverityColor(discrepancy.severity)}>
        {t(`discrepancy.severity.${discrepancy.severity}`)}
      </DotBadge>
      <DotBadge color={toDiscrepancyStatusColor(discrepancy.status)}>
        {t(`discrepancy.status.${discrepancy.status}`)}
      </DotBadge>
    </Group>
  );
}
