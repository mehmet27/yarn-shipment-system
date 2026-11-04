import type { Weighing } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Badge, Group, Table, Text } from '@iplik/ui';
import { formatKg, formatPct, isKilogram } from '@iplik/units';

/** Çıkış tartısının standart dara sapması; standart tanımsızsa satır gösterilmez. */
export function TareDeviationRow({ weighing }: { weighing: Weighing | undefined }) {
  const { t } = useTranslation();
  if (weighing?.tareDeviationPct === undefined) return null;
  const expected =
    weighing.expectedTareKg !== undefined && isKilogram(weighing.expectedTareKg)
      ? formatKg(weighing.expectedTareKg)
      : undefined;
  return (
    <Table.Tr>
      <Table.Th scope="row">{t('shipment.file.tareDeviation')}</Table.Th>
      <Table.Td ta="right">
        <Group gap="xs" justify="flex-end">
          <Text span fw={600}>
            {formatPct(weighing.tareDeviationPct)}
          </Text>
          {weighing.tareAlert === true && (
            <Badge color="red" variant="light">
              {t('shipment.file.tareAlert')}
            </Badge>
          )}
        </Group>
        {expected !== undefined && weighing.tareTolerancePct !== undefined && (
          <Text size="xs" c="dimmed">
            {t('shipment.file.tareExpected', {
              expected,
              tolerance: formatPct(weighing.tareTolerancePct).slice(1),
            })}
          </Text>
        )}
      </Table.Td>
      <Table.Td />
    </Table.Tr>
  );
}
