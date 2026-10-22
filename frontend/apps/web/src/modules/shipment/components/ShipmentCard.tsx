import type { ShipmentSummary } from '@iplik/api-client';
import { formatDateTime } from '@iplik/i18n';
import { Group, Paper, Stack, Text } from '@iplik/ui';
import { Link } from 'react-router';
import { ShipmentStatusBadge } from './ShipmentStatusBadge';
import { toStatusColor } from './statusColor';

export type ShipmentCardProps = {
  shipment: ShipmentSummary;
  to: string;
};

/** Dar ekranda liste satırı: tek dokunuşla açılan büyük kart (FE-UI-07, en az 44 px hedef). */
export function ShipmentCard({ shipment, to }: ShipmentCardProps) {
  return (
    <Paper
      component={Link}
      to={to}
      withBorder
      p="md"
      mih={72}
      c="dark"
      aria-label={shipment.shipmentNo}
      style={{
        textDecoration: 'none',
        borderLeft: `4px solid var(--mantine-color-${toStatusColor(shipment.status)}-5)`,
      }}
    >
      <Stack gap={4}>
        <Group justify="space-between">
          <Text fw={700} size="lg">
            {shipment.shipmentNo}
          </Text>
          <ShipmentStatusBadge status={shipment.status} />
        </Group>
        <Text fw={500}>{shipment.subcontractorTitle}</Text>
        <Text size="sm" c="dimmed">
          {shipment.plateNo} · {formatDateTime(shipment.createdAt)}
        </Text>
      </Stack>
    </Paper>
  );
}
