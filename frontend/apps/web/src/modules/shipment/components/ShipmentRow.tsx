import type { ShipmentSummary } from '@iplik/api-client';
import { formatDate, formatDateTime, useTranslation } from '@iplik/i18n';
import { Anchor, Stack, Table } from '@iplik/ui';
import { Link } from 'react-router';
import { ShipmentProgress } from './ShipmentProgress';
import { ShipmentStatusBadge } from './ShipmentStatusBadge';

/** Masaüstü tablosunun tek satırı. */
export function ShipmentRow({ shipment }: { shipment: ShipmentSummary }) {
  const { t } = useTranslation();
  return (
    <Table.Tr>
      <Table.Td style={{ whiteSpace: 'nowrap' }}>
        <Anchor component={Link} to={`/panel/sevkiyat/${shipment.shipmentId}`}>
          {shipment.shipmentNo}
        </Anchor>
      </Table.Td>
      <Table.Td>
        <Stack gap={6} align="flex-start">
          <ShipmentStatusBadge status={shipment.status} />
          <ShipmentProgress status={shipment.status} />
        </Stack>
      </Table.Td>
      <Table.Td c="brand.8" fw={500}>
        {t(`shipment.nextStep.${shipment.status}`)}
      </Table.Td>
      <Table.Td>{shipment.subcontractorTitle}</Table.Td>
      <Table.Td>{shipment.driverName}</Table.Td>
      <Table.Td style={{ whiteSpace: 'nowrap' }}>{shipment.plateNo}</Table.Td>
      <Table.Td style={{ whiteSpace: 'nowrap' }}>{formatDate(shipment.shipmentDate)}</Table.Td>
      <Table.Td style={{ whiteSpace: 'nowrap' }}>{formatDateTime(shipment.createdAt)}</Table.Td>
    </Table.Tr>
  );
}
