import type { ShipmentDetail } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Group, Paper, SimpleGrid, Stack, Text, Title } from '@iplik/ui';
import { ShipmentStatusBadge } from './ShipmentStatusBadge';

export type ShipmentHeaderProps = {
  shipment: ShipmentDetail;
};

/** Bölüm 19: sevkiyat dosyasının üst kartı; fasoncu, şoför, araç ve durum. */
export function ShipmentHeader({ shipment }: ShipmentHeaderProps) {
  const { t } = useTranslation();
  const facts = [
    { labelKey: 'shipment.field.subcontractor', value: shipment.subcontractorTitle },
    { labelKey: 'shipment.field.driver', value: shipment.driverName },
    { labelKey: 'shipment.field.plate', value: shipment.plateNo },
    { labelKey: 'shipment.field.sealNo', value: shipment.sealNo ?? t('common.notRecorded') },
  ];
  return (
    <Paper withBorder p="md">
      <Stack>
        <Group>
          <Title order={2}>{shipment.shipmentNo}</Title>
          <ShipmentStatusBadge status={shipment.status} />
        </Group>
        <SimpleGrid cols={{ base: 2, md: 4 }}>
          {facts.map(({ labelKey, value }) => (
            <Stack key={labelKey} gap={2}>
              <Text size="sm" c="dimmed">
                {t(labelKey)}
              </Text>
              <Text>{value}</Text>
            </Stack>
          ))}
        </SimpleGrid>
      </Stack>
    </Paper>
  );
}
