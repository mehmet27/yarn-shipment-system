import type { ShipmentDetail } from '@iplik/api-client';
import { formatDate, useTranslation } from '@iplik/i18n';
import { Button, Group, Paper, SimpleGrid, Stack, Text, Title } from '@iplik/ui';
import { Link } from 'react-router';
import { ShipmentGuide } from '../guide/ShipmentGuide';
import { NotDueNotice } from './NotDueNotice';
import { findDriverName } from './shipmentViews';
import { useFieldText } from './useFieldText';
import { ShipmentStatusBadge } from './ShipmentStatusBadge';

export type ShipmentHeaderProps = {
  shipment: ShipmentDetail;
};

/** Sevkiyat dosyasının üst kartı; fasoncu, şoför, araç, irsaliye ve durum. */
export function ShipmentHeader({ shipment }: ShipmentHeaderProps) {
  const { t } = useTranslation();
  const text = useFieldText();
  const source =
    'sourceSubcontractorTitle' in shipment ? shipment.sourceSubcontractorTitle : undefined;
  const facts = [
    { labelKey: 'shipment.field.shipmentDate', value: formatDate(shipment.shipmentDate) },
    { labelKey: 'shipment.field.subcontractor', value: shipment.subcontractorTitle },
    ...(source === undefined ? [] : [{ labelKey: 'shipment.field.source', value: source }]),
    { labelKey: 'shipment.field.driver', value: text(findDriverName(shipment)) },
    { labelKey: 'shipment.field.plate', value: shipment.plateNo ?? t('common.notRecorded') },
    {
      labelKey: 'shipment.field.waybillNo',
      value: ('waybillNo' in shipment ? shipment.waybillNo : undefined) ?? t('common.notRecorded'),
    },
  ];
  return (
    <Paper withBorder style={{ overflow: 'hidden' }}>
      <Stack gap="md" p="md">
        <Group justify="space-between" gap="sm">
          <Group gap="sm">
            <Title order={1}>{shipment.shipmentNo}</Title>
            <ShipmentStatusBadge status={shipment.status} />
          </Group>
          {shipment.status !== 'CANCELLED' && (
            <Button component={Link} to="ambar-cikis-fisi" relative="path" variant="default">
              {t('shipment.exitSlip.open')}
            </Button>
          )}
        </Group>
        <NotDueNotice shipment={shipment} />
        <ShipmentGuide shipment={shipment} />
      </Stack>
      <SimpleGrid
        cols={{ base: 2, md: 4 }}
        p="md"
        style={{ borderTop: '1px solid var(--app-line)' }}
      >
        {facts.map(({ labelKey, value }) => (
          <Stack key={labelKey} gap={2}>
            <Text size="xs" c="dimmed">
              {t(labelKey)}
            </Text>
            <Text fw={500}>{value}</Text>
          </Stack>
        ))}
      </SimpleGrid>
    </Paper>
  );
}
