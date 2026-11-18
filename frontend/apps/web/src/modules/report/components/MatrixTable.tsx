import { useTranslation } from '@iplik/i18n';
import { Table } from '@iplik/ui';
import { useYarnLabel } from '@/modules/shipment';
import { formatKg3 } from '../format';
import type { BalanceMatrix } from '../pivot';

const MIN_WIDTH = 560;

export function MatrixTable({ matrix }: { matrix: BalanceMatrix }) {
  const { t } = useTranslation();
  const yarnLabel = useYarnLabel();
  return (
    <Table.ScrollContainer minWidth={MIN_WIDTH}>
      <Table>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>{t('report.matrix.yarnType')}</Table.Th>
            {matrix.columns.map((column) => (
              <Table.Th key={column.id} ta="right">
                {column.name}
              </Table.Th>
            ))}
            <Table.Th ta="right">{t('report.export.total')}</Table.Th>
            <Table.Th ta="right">{t('report.matrix.inTransit')}</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {matrix.rows.map((row) => (
            <Table.Tr key={row.yarnType}>
              <Table.Td>{yarnLabel(row.yarnType)}</Table.Td>
              {matrix.columns.map((column) => (
                <Table.Td key={column.id} ta="right">
                  {formatKg3(row.values[column.id] ?? 0)}
                </Table.Td>
              ))}
              <Table.Td ta="right" fw={600}>
                {formatKg3(row.totalKg)}
              </Table.Td>
              <Table.Td ta="right">{formatKg3(row.inTransitKg)}</Table.Td>
            </Table.Tr>
          ))}
          <Table.Tr fw={600}>
            <Table.Td>{t('report.export.total')}</Table.Td>
            {matrix.columns.map((column) => (
              <Table.Td key={column.id} ta="right">
                {formatKg3(matrix.columnTotals[column.id] ?? 0)}
              </Table.Td>
            ))}
            <Table.Td ta="right">{formatKg3(matrix.grandTotalKg)}</Table.Td>
            <Table.Td ta="right">{formatKg3(matrix.grandInTransitKg)}</Table.Td>
          </Table.Tr>
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  );
}
