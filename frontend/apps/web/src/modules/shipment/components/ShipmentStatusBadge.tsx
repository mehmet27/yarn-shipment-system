import type { ShipmentStatus } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Badge } from '@iplik/ui';

export type ShipmentStatusBadgeProps = {
  status: ShipmentStatus;
};

/** FE-UI-03: durum renkle birlikte metinle de verilir. */
function toColor(status: ShipmentStatus): string {
  switch (status) {
    case 'DRAFT':
      return 'gray';
    case 'LOADED':
    case 'IN_TRANSIT':
      return 'blue';
    case 'AWAITING_DELIVERY':
    case 'CHECKING':
      return 'yellow';
    case 'IN_REVIEW':
      return 'orange';
    case 'COMPLETED':
      return 'green';
    case 'CANCELLED':
      return 'red';
  }
}

export function ShipmentStatusBadge({ status }: ShipmentStatusBadgeProps) {
  const { t } = useTranslation();
  return (
    <Badge color={toColor(status)} variant="light">
      {t(`shipment.status.${status}`)}
    </Badge>
  );
}
