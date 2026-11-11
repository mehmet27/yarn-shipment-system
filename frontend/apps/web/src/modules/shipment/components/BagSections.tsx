import type { ShipmentBagsResponse } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { formatKgText, sumKg } from '@iplik/units';
import { Button, Group, Paper, Stack, Text, Title } from '@iplik/ui';

/** Sevkiyattaki çuvallar: sıra no ve kg; altta toplam kg. */
export function BagSections({
  bags,
  onRemove,
}: {
  bags: ShipmentBagsResponse;
  onRemove?: ((seqNo: number) => void) | undefined;
}) {
  const { t } = useTranslation();
  const { dispatchBags } = bags;
  return (
    <Stack gap="xs">
      <Title order={5}>{t('shipment.bags.dispatch', { count: dispatchBags.length })}</Title>
      {dispatchBags.map((bag) => (
        <Paper key={bag.seqNo} withBorder p="sm">
          <Group justify="space-between" wrap="nowrap">
            <Text fw={600}>
              {t('shipment.bags.seq', { seq: bag.seqNo })} · {formatKgText(bag.weightKg)}
            </Text>
            {onRemove !== undefined && (
              <Button
                color="red"
                variant="subtle"
                onClick={() => {
                  onRemove(bag.seqNo);
                }}
              >
                {t('shipment.bags.removeFromShipment')}
              </Button>
            )}
          </Group>
        </Paper>
      ))}
      {dispatchBags.length > 0 && (
        <Text size="sm" c="dimmed">
          {t('shipment.bags.total', {
            kg: formatKgText(sumKg(dispatchBags.map((b) => b.weightKg))),
          })}
        </Text>
      )}
    </Stack>
  );
}
