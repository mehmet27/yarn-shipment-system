import type { ShipmentStatus } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Badge, Box } from '@iplik/ui';
import { toStatusColor } from './statusColor';

export type ShipmentStatusBadgeProps = {
  status: ShipmentStatus;
};

const DOT_SIZE_PX = 8;

/** Durum rozeti: nötr kenarlı, solunda durum renginde nokta (FE-UI-03: renk metinle birlikte verilir). */
export function ShipmentStatusBadge({ status }: ShipmentStatusBadgeProps) {
  const { t } = useTranslation();
  return (
    <Badge
      variant="outline"
      color="gray"
      radius="xl"
      data-testid="shipment-status"
      c="var(--app-text)"
      style={{ borderColor: 'var(--app-line)', whiteSpace: 'nowrap' }}
      leftSection={
        <Box
          w={DOT_SIZE_PX}
          h={DOT_SIZE_PX}
          bg={`var(--mantine-color-${toStatusColor(status)}-filled)`}
          style={{ borderRadius: '50%' }}
        />
      }
    >
      {t(`shipment.status.${status}`)}
    </Badge>
  );
}
