import type { DispatchBagView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { formatKgText, sumKg } from '@iplik/units';
import { Table, Text } from '@iplik/ui';
import { Fragment } from 'react';

const COLUMNS = 3;
const COLUMN_IDS = ['first', 'second', 'third'] as const;

/** Çuvalları üç sütuna böler; her sütun art arda gelen çuvalları içerir (1–20, 21–40, 41–60). */
function splitColumns(bags: readonly DispatchBagView[]): DispatchBagView[][] {
  const rows = Math.ceil(bags.length / COLUMNS);
  return Array.from({ length: COLUMNS }, (_, c) => bags.slice(c * rows, (c + 1) * rows));
}

/** Kodsuz çuval listesi: üç sütunda sıra no + kg; altta sütun toplamları, çuval adedi ve genel toplam. */
export function ExitSlipBags({ bags }: { bags: readonly DispatchBagView[] }) {
  const { t } = useTranslation();
  if (bags.length === 0) return <Text c="dimmed">{t('shipment.exitSlip.noBags')}</Text>;
  const columns = splitColumns(bags);
  const rowCount = columns[0]?.length ?? 0;
  const total = formatKgText(sumKg(bags.map((bag) => bag.weightKg)));
  return (
    <Table withTableBorder withColumnBorders data-testid="exit-slip-bags">
      <Table.Thead>
        <Table.Tr>
          {COLUMN_IDS.map((id) => (
            <Table.Th key={id} colSpan={2} ta="center">
              {t('shipment.exitSlip.seq')} / {t('shipment.exitSlip.kg')}
            </Table.Th>
          ))}
        </Table.Tr>
      </Table.Thead>
      <Table.Tbody>
        {Array.from({ length: rowCount }, (_, r) => (
          <Table.Tr key={columns[0]?.[r]?.seqNo}>
            {columns.map((column, c) => (
              <Fragment key={COLUMN_IDS[c]}>
                <Table.Td w={50}>{column[r]?.seqNo}</Table.Td>
                <Table.Td ta="right">
                  {column[r] === undefined ? '' : formatKgText(column[r].weightKg)}
                </Table.Td>
              </Fragment>
            ))}
          </Table.Tr>
        ))}
        <Table.Tr>
          {columns.map((column, c) => (
            <Table.Th key={COLUMN_IDS[c]} colSpan={2} ta="right">
              {formatKgText(sumKg(column.map((bag) => bag.weightKg)))}
            </Table.Th>
          ))}
        </Table.Tr>
        <Table.Tr>
          <Table.Th colSpan={COLUMNS * 2} ta="right">
            {t('shipment.exitSlip.total', { count: bags.length })}: {total}
          </Table.Th>
        </Table.Tr>
      </Table.Tbody>
    </Table>
  );
}
