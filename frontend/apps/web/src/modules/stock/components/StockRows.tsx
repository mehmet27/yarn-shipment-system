import { useTranslation } from '@iplik/i18n';
import type { CentralStock } from '@iplik/api-client/central-stock';
import { Table, Text } from '@iplik/ui';
import { formatSignedKg } from '@/modules/shipment';

/** Merkezde iplik cinsi başına eldeki kilo. */
export function StockRows({ stock }: { stock: CentralStock }) {
  const { t } = useTranslation();
  if (stock.rows.length === 0) return <Text c="dimmed">{t('stock.empty')}</Text>;
  return (
    <Table.ScrollContainer minWidth={360}>
      <Table striped aria-label={t('stock.title')}>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>{t('stock.yarnType')}</Table.Th>
            <Table.Th>{t('stock.stockKg')}</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {stock.rows.map((row) => (
            <Table.Tr key={row.yarnType}>
              <Table.Td>{row.yarnType}</Table.Td>
              <Table.Td>{formatSignedKg(row.stockKg)}</Table.Td>
            </Table.Tr>
          ))}
          <Table.Tr>
            <Table.Th>{t('stock.total')}</Table.Th>
            <Table.Th>{formatSignedKg(stock.totalKg)}</Table.Th>
          </Table.Tr>
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  );
}
