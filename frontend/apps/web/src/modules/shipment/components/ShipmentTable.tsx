import type { ShipmentSummary } from '@iplik/api-client';
import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Anchor, Stack, Table, useMediaQuery } from '@iplik/ui';
import { Link } from 'react-router';
import { ShipmentCard } from './ShipmentCard';
import { ShipmentStatusBadge } from './ShipmentStatusBadge';

const DESKTOP_QUERY = '(min-width: 48em)';

export type ShipmentTableProps = {
  shipments: readonly ShipmentSummary[];
};

/** Mobil öncelikli (FE-UI-07): dar ekranda kartlar, geniş ekranda tablo. */
export function ShipmentTable({ shipments }: ShipmentTableProps) {
  const { t } = useTranslation();
  const isDesktop = useMediaQuery(DESKTOP_QUERY, true, { getInitialValueInEffect: false });
  if (!isDesktop) {
    return (
      <Stack gap="sm">
        {shipments.map((shipment) => (
          <ShipmentCard
            key={shipment.shipmentId}
            shipment={shipment}
            to={`/panel/sevkiyat/${shipment.shipmentId}`}
          />
        ))}
      </Stack>
    );
  }
  return (
    <Table.ScrollContainer minWidth={720}>
      <Table striped highlightOnHover>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>{t('shipment.field.shipmentNo')}</Table.Th>
            <Table.Th>{t('shipment.field.status')}</Table.Th>
            <Table.Th>{t('shipment.field.subcontractor')}</Table.Th>
            <Table.Th>{t('shipment.field.driver')}</Table.Th>
            <Table.Th>{t('shipment.field.plate')}</Table.Th>
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
              <Table.Td>{formatDateTime(shipment.createdAt)}</Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  );
}
