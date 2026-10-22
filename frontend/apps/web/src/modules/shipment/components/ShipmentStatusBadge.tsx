import type { ShipmentStatus } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Badge } from '@iplik/ui';
import { toStatusColor } from './statusColor';

export type ShipmentStatusBadgeProps = {
  status: ShipmentStatus;
};

export function ShipmentStatusBadge({ status }: ShipmentStatusBadgeProps) {
  const { t } = useTranslation();
  return (
    <Badge color={toStatusColor(status)} variant="light">
      {t(`shipment.status.${status}`)}
    </Badge>
  );
}
