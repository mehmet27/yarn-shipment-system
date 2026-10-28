import type { Weighing } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Stack, Table, Text } from '@iplik/ui';
import type { Kilogram } from '@iplik/units';
import { calculateDifferencePct, formatKg, formatPct } from '@iplik/units';
import type { ShipmentWeighings } from './shipmentViews';

export type WeightComparisonProps = {
  weighings: ShipmentWeighings;
};

type WeightField = 'grossKg' | 'tareKg' | 'netKg';
const WEIGHT_ROWS: readonly { field: WeightField; labelKey: string }[] = [
  { field: 'grossKg', labelKey: 'shipment.file.gross' },
  { field: 'tareKg', labelKey: 'shipment.file.tare' },
  { field: 'netKg', labelKey: 'shipment.file.net' },
];

/** Gizli taraf için tire; "Kaydedilmedi" yazmak yanlış bilgi olur (H-14). */
const HIDDEN_CELL = '—';

function formatWeight(weighing: Weighing | undefined, field: WeightField): string | undefined {
  return weighing === undefined ? undefined : formatKg(weighing[field]);
}

/** Rapor 9.1: fark yüzdesi yalnızca iki taraf da tartıldıysa gösterilir. */
function differencePct(dispatchNetKg?: Kilogram, deliveryNetKg?: Kilogram): string | undefined {
  if (dispatchNetKg === undefined || deliveryNetKg === undefined) return undefined;
  return formatPct(calculateDifferencePct(dispatchNetKg, deliveryNetKg));
}

export function WeightComparison({ weighings }: WeightComparisonProps) {
  const { t } = useTranslation();
  const { dispatchWeighing, deliveryWeighing, hidden } = weighings;
  const notRecorded = t('common.notRecorded');
  const cell = (weighing: Weighing | undefined, isHidden: boolean, field: WeightField) =>
    isHidden ? HIDDEN_CELL : (formatWeight(weighing, field) ?? notRecorded);
  const difference = differencePct(dispatchWeighing?.netKg, deliveryWeighing?.netKg);
  const isAnyHidden = hidden.dispatch || hidden.delivery;
  return (
    <Stack gap="xs" maw={640}>
      <Table>
        <Table.Thead>
          <Table.Tr>
            <Table.Th />
            <Table.Th ta="right">{t('shipment.file.dispatch')}</Table.Th>
            <Table.Th ta="right">{t('shipment.file.delivery')}</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {WEIGHT_ROWS.map(({ field, labelKey }) => (
            <Table.Tr key={field}>
              <Table.Th scope="row">{t(labelKey)}</Table.Th>
              <Table.Td ta="right">{cell(dispatchWeighing, hidden.dispatch, field)}</Table.Td>
              <Table.Td ta="right">{cell(deliveryWeighing, hidden.delivery, field)}</Table.Td>
            </Table.Tr>
          ))}
          <Table.Tr>
            <Table.Th scope="row">{t('shipment.file.difference')}</Table.Th>
            <Table.Td colSpan={2} ta="right">
              <Text fw={600}>{isAnyHidden ? HIDDEN_CELL : (difference ?? notRecorded)}</Text>
            </Table.Td>
          </Table.Tr>
        </Table.Tbody>
      </Table>
      {isAnyHidden && (
        <Text size="sm" c="dimmed">
          {t(hidden.delivery ? 'shipment.file.deliveryHidden' : 'shipment.file.dispatchHidden')}
        </Text>
      )}
    </Stack>
  );
}
