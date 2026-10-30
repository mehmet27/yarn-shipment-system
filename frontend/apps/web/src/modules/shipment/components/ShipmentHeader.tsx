import type { ShipmentDetail } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Group, Paper, SimpleGrid, Stack, Text, Title } from '@iplik/ui';
import { findDriverName, findSealNo } from './shipmentViews';
import { useFieldText } from './useFieldText';
import { ShipmentStatusBadge } from './ShipmentStatusBadge';
import { toStatusColor } from './statusColor';

export type ShipmentHeaderProps = {
  shipment: ShipmentDetail;
};

/** Bölüm 19: sevkiyat dosyasının üst kartı; fasoncu, şoför, araç ve durum. */
export function ShipmentHeader({ shipment }: ShipmentHeaderProps) {
  const { t } = useTranslation();
  const text = useFieldText();
  const facts = [
    { labelKey: 'shipment.field.subcontractor', value: shipment.subcontractorTitle },
    { labelKey: 'shipment.field.driver', value: text(findDriverName(shipment)) },
    { labelKey: 'shipment.field.plate', value: shipment.plateNo ?? t('common.notRecorded') },
    { labelKey: 'shipment.field.sealNo', value: text(findSealNo(shipment)) },
  ];
  return (
    <Paper
      withBorder
      p="md"
      style={{ borderTop: `4px solid var(--mantine-color-${toStatusColor(shipment.status)}-5)` }}
    >
      <Stack>
        <Group>
          <Title order={1} size="h2">
            {shipment.shipmentNo}
          </Title>
          <ShipmentStatusBadge status={shipment.status} />
        </Group>
        <SimpleGrid cols={{ base: 2, md: 4 }}>
          {facts.map(({ labelKey, value }) => (
            <Stack key={labelKey} gap={2}>
              <Text size="sm" c="dimmed">
                {t(labelKey)}
              </Text>
              <Text fw={500}>{value}</Text>
            </Stack>
          ))}
        </SimpleGrid>
      </Stack>
    </Paper>
  );
}
