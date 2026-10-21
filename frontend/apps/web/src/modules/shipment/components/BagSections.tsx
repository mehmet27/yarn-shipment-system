import type { ShipmentBagsResponse } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Badge, Paper, Stack, Text, Title } from '@iplik/ui';
import { formatDateTime } from './formatDateTime';

/** Backend'in verdiği listeler çizilir; hangisinin geleceğine rol değil yanıt karar verir. */
export function BagSections({ bags }: { bags: ShipmentBagsResponse }) {
  const { t } = useTranslation();
  const { dispatchBags, deliveryScans } = bags;
  return (
    <Stack>
      {dispatchBags !== undefined && (
        <Stack gap="xs">
          <Title order={5}>{t('shipment.bags.dispatch', { count: dispatchBags.length })}</Title>
          {dispatchBags.map((bag) => (
            <Paper key={bag.bagId} withBorder p="sm">
              <Text fw={600}>{bag.uniqueKey}</Text>
              <Text size="sm" c="dimmed">
                {t('shipment.bags.lot')} {bag.lotNo} · {bag.qualityGradeCode}
              </Text>
            </Paper>
          ))}
        </Stack>
      )}
      {deliveryScans !== undefined && (
        <Stack gap="xs">
          <Title order={5}>{t('shipment.bags.delivery', { count: deliveryScans.length })}</Title>
          {deliveryScans.map((scan) => (
            <Paper key={`${scan.scannedKey}-${scan.scannedAt}`} withBorder p="sm">
              <Text fw={600}>{scan.scannedKey}</Text>
              <Text size="sm" c="dimmed">
                {formatDateTime(scan.scannedAt)}
              </Text>
              {scan.matched !== undefined && (
                <Badge color={scan.matched ? 'green' : 'red'}>
                  {t(scan.matched ? 'shipment.bags.matched' : 'shipment.bags.unmatched')}
                </Badge>
              )}
            </Paper>
          ))}
        </Stack>
      )}
    </Stack>
  );
}
