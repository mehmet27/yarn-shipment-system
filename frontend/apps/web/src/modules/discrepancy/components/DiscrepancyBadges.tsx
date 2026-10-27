import type { DiscrepancyView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Badge, Group } from '@iplik/ui';
import { toDiscrepancyStatusColor, toSeverityColor } from './discrepancyColor';

export function DiscrepancyBadges({ discrepancy }: { discrepancy: DiscrepancyView }) {
  const { t } = useTranslation();
  return (
    <Group gap="xs">
      <Badge color={toSeverityColor(discrepancy.severity)}>
        {t(`discrepancy.severity.${discrepancy.severity}`)}
      </Badge>
      <Badge variant="light" color={toDiscrepancyStatusColor(discrepancy.status)}>
        {t(`discrepancy.status.${discrepancy.status}`)}
      </Badge>
    </Group>
  );
}
