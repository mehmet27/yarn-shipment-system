import type { ShipmentSummary } from '@iplik/api-client';
import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Anchor, Table } from '@iplik/ui';
import { formatKg } from '@iplik/units';
import { Link } from 'react-router';
import { ShipmentStatusBadge } from './ShipmentStatusBadge';

export type ShipmentTableProps = {
  shipments: readonly ShipmentSummary[];
};

export function ShipmentTable({ shipments }: ShipmentTableProps) {
  const { t } = useTranslation();
  return (
    <Table.ScrollContainer minWidth={900}>
      <Table striped highlightOnHover>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>{t('shipment.field.shipmentNo')}</Table.Th>
            <Table.Th>{t('shipment.field.status')}</Table.Th>
            <Table.Th>{t('shipment.field.subcontractor')}</Table.Th>
            <Table.Th>{t('shipment.field.driver')}</Table.Th>
            <Table.Th>{t('shipment.field.plate')}</Table.Th>
            <Table.Th ta="right">{t('shipment.field.bagCount')}</Table.Th>
            <Table.Th ta="right">{t('shipment.field.plannedNetKg')}</Table.Th>
            <Table.Th>{t('shipment.field.createdAt')}</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {shipments.map((shipment) => (
            <Table.Tr key={shipment.shipmentId}>
              <Table.Td>
                <Anchor component={Link} to={`/panel/sevkiyat/${shipment.shipmentId}`}>
                  {shipment.shipmentNo}
                </Anchor>
              </Table.Td>
              <Table.Td>
                <ShipmentStatusBadge status={shipment.status} />
              </Table.Td>
              <Table.Td>{shipment.subcontractorTitle}</Table.Td>
              <Table.Td>{shipment.driverName}</Table.Td>
              <Table.Td>{shipment.plateNo}</Table.Td>
              <Table.Td ta="right">{shipment.plannedBagCount}</Table.Td>
              <Table.Td ta="right">{formatKg(shipment.plannedNetKg)}</Table.Td>
              <Table.Td>{formatDateTime(shipment.createdAt)}</Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  );
}
