import type { RecipeLineView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { formatKgText } from '@iplik/units';
import { Table } from '@iplik/ui';

/** Reçetedeki iplik cinsleri ve planlanan net kg. */
export function ExitSlipRecipe({ lines }: { lines: readonly RecipeLineView[] }) {
  const { t } = useTranslation();
  return (
    <Table withTableBorder withColumnBorders>
      <Table.Thead>
        <Table.Tr>
          <Table.Th>{t('shipment.exitSlip.yarnType')}</Table.Th>
          <Table.Th ta="right">{t('shipment.exitSlip.plannedKg')}</Table.Th>
        </Table.Tr>
      </Table.Thead>
      <Table.Tbody>
        {lines.map((line, index) => (
          <Table.Tr key={`${line.yarnTypeText ?? line.yarnTypeId ?? ''}-${String(index)}`}>
            <Table.Td>{line.yarnTypeText ?? line.yarnTypeId ?? '—'}</Table.Td>
            <Table.Td ta="right">{formatKgText(line.netKg)}</Table.Td>
          </Table.Tr>
        ))}
      </Table.Tbody>
    </Table>
  );
}
