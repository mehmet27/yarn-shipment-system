import type { DispatchBagView, ShipmentBagsResponse } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Button, Group, Paper, Stack, Text, Title } from '@iplik/ui';
import { DeliveryScanRow } from './DeliveryScanRow';
import { TypeSummary } from './TypeSummary';

/** Backend'in verdiği listeler çizilir; hangisinin geleceğine rol değil yanıt karar verir. */
export function BagSections({
  bags,
  onRemove,
}: {
  bags: ShipmentBagsResponse;
  onRemove?: ((uniqueKey: string) => void) | undefined;
}) {
  const { t } = useTranslation();
  const { dispatchBags, deliveryScans } = bags;
  return (
    <Stack>
      {dispatchBags !== undefined && (
        <Stack gap="xs">
          <Title order={5}>{t('shipment.bags.dispatch', { count: dispatchBags.length })}</Title>
          <TypeSummary types={dispatchBags.map((bag) => bag.packageType)} />
          {dispatchBags.map((bag) => (
            <Paper key={bag.bagId} withBorder p="sm">
              <Group justify="space-between" wrap="nowrap">
                <div>
                  <Text fw={600}>{bag.uniqueKey}</Text>
                  <Text size="sm" c="dimmed">
                    {bagDetail(t, bag)}
                  </Text>
                </div>
                {onRemove !== undefined && (
                  <Button
                    color="red"
                    variant="subtle"
                    onClick={() => {
                      onRemove(bag.uniqueKey);
                    }}
                  >
                    {t('shipment.bags.removeFromShipment')}
                  </Button>
                )}
              </Group>
            </Paper>
          ))}
        </Stack>
      )}
      {deliveryScans !== undefined && (
        <Stack gap="xs">
          <Title order={5}>{t('shipment.bags.delivery', { count: deliveryScans.length })}</Title>
          <TypeSummary types={deliveryScans.flatMap((scan) => scan.packageType ?? [])} />
          {deliveryScans.map((scan) => (
            <DeliveryScanRow key={`${scan.scannedKey}-${scan.scannedAt}`} scan={scan} />
          ))}
        </Stack>
      )}
    </Stack>
  );
}

/** Çıkış listesindeki satırın ikinci satırı: ambalaj türü, parti ve kalite. */
type Translate = ReturnType<typeof useTranslation>['t'];

function bagDetail(t: Translate, bag: DispatchBagView): string {
  const type = t(`bag.packageTypes.${bag.packageType}`);
  return `${type} · ${t('shipment.bags.lot')} ${bag.lotNo} · ${bag.qualityGradeCode}`;
}
