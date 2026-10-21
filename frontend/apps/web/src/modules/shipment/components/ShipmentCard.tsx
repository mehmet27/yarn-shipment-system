import type { ShipmentSummary } from '@iplik/api-client';
import { formatDateTime } from '@iplik/i18n';
import { Group, Paper, Stack, Text } from '@iplik/ui';
import { Link } from 'react-router';
import { ShipmentStatusBadge } from './ShipmentStatusBadge';

export type ShipmentCardProps = {
  shipment: ShipmentSummary;
  to: string;
};

/** Dar ekranda liste satırı: tek dokunuşla açılan büyük kart (FE-UI-07, en az 44 px hedef). */
export function ShipmentCard({ shipment, to }: ShipmentCardProps) {
  return (
    <Paper component={Link} to={to} withBorder p="md" mih={72} aria-label={shipment.shipmentNo}>
      <Stack gap={4}>
        <Group justify="space-between">
          <Text fw={700} size="lg">
            {shipment.shipmentNo}
          </Text>
          <ShipmentStatusBadge status={shipment.status} />
        </Group>
        <Text>{shipment.subcontractorTitle}</Text>
        <Text size="sm" c="dimmed">
          {shipment.plateNo} · {formatDateTime(shipment.createdAt)}
        </Text>
      </Stack>
    </Paper>
  );
}
