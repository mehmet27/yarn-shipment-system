import type { ShipmentSummary } from '@iplik/api-client';
import { formatDateTime } from '@iplik/i18n';
import { Group, Paper, Stack, Text } from '@iplik/ui';
import { Link } from 'react-router';
import { ShipmentStatusBadge } from './ShipmentStatusBadge';

export type FieldShipmentCardProps = {
  shipment: ShipmentSummary;
};

/** Saha listesinde tek dokunuşla açılan büyük kart (en az 48 px dokunma hedefi). */
export function FieldShipmentCard({ shipment }: FieldShipmentCardProps) {
  return (
    <Paper
      component={Link}
      to={`/saha/sevkiyat/${shipment.shipmentId}`}
      withBorder
      p="md"
      mih={72}
      aria-label={shipment.shipmentNo}
    >
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
