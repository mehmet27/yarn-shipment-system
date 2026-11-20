import type { ShipmentBagsResponse } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { formatKgText, sumKg } from '@iplik/units';
import { Button, Paper, Stack, Table, Text, Title } from '@iplik/ui';

/** Sevkiyattaki çuvallar: sıra no ve kg tabloda; altta toplam kg. */
export function BagSections({
  bags,
  onRemove,
  removingSeqNo,
}: {
  bags: ShipmentBagsResponse;
  onRemove?: ((seqNo: number) => void) | undefined;
  /** Çıkarılmakta olan çuval; istek bitene kadar düğmeler ikinci tıklamayı almaz. */
  removingSeqNo?: number | undefined;
}) {
  const { t } = useTranslation();
  const { dispatchBags } = bags;
  return (
    <Stack gap="xs">
      <Title order={3}>{t('shipment.bags.dispatch', { count: dispatchBags.length })}</Title>
      {dispatchBags.length > 0 && (
        <Paper withBorder style={{ overflow: 'hidden' }}>
          <Table.ScrollContainer minWidth={0}>
            <Table>
              <Table.Tbody>
                {dispatchBags.map((bag) => (
                  <Table.Tr key={bag.seqNo}>
                    <Table.Td>{t('shipment.bags.seq', { seq: bag.seqNo })}</Table.Td>
                    <Table.Td ta="right" fw={600} style={{ whiteSpace: 'nowrap' }}>
                      {formatKgText(bag.weightKg)}
                    </Table.Td>
                    {onRemove !== undefined && (
                      <Table.Td ta="right">
                        <Button
                          color="red"
                          variant="subtle"
                          style={{ whiteSpace: 'nowrap' }}
                          loading={removingSeqNo === bag.seqNo}
                          disabled={removingSeqNo !== undefined && removingSeqNo !== bag.seqNo}
                          onClick={() => {
                            onRemove(bag.seqNo);
                          }}
                        >
                          {t('shipment.bags.removeFromShipment')}
                        </Button>
                      </Table.Td>
                    )}
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
        </Paper>
      )}
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
