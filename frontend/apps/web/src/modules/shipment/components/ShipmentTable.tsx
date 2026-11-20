import type { ShipmentSummary } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Paper, Stack, Table } from '@iplik/ui';
import { useWideLayout } from '@/modules/shared';
import { ShipmentRow } from './ShipmentRow';
import { ShipmentCard } from './ShipmentCard';

export type ShipmentTableProps = {
  shipments: readonly ShipmentSummary[];
};

/** Mobil öncelikli (FE-UI-07): dar ekranda kartlar, geniş ekranda tablo. */
export function ShipmentTable({ shipments }: ShipmentTableProps) {
  const { t } = useTranslation();
  const isDesktop = useWideLayout();
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
    <Paper withBorder style={{ overflow: 'hidden' }}>
      <Table.ScrollContainer minWidth={1000}>
        <Table highlightOnHover horizontalSpacing="md">
          <Table.Thead>
            <Table.Tr>
              <Table.Th>{t('shipment.field.shipmentNo')}</Table.Th>
              <Table.Th>{t('shipment.field.status')}</Table.Th>
              <Table.Th>{t('shipment.nextStep.label')}</Table.Th>
              <Table.Th>{t('shipment.field.subcontractor')}</Table.Th>
              <Table.Th>{t('shipment.field.driver')}</Table.Th>
              <Table.Th>{t('shipment.field.plate')}</Table.Th>
              <Table.Th>{t('shipment.field.shipmentDate')}</Table.Th>
              <Table.Th>{t('shipment.field.createdAt')}</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {shipments.map((shipment) => (
              <ShipmentRow key={shipment.shipmentId} shipment={shipment} />
            ))}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    </Paper>
  );
}
