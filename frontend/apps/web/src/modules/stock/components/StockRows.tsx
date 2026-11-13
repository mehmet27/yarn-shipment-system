import { useTranslation } from '@iplik/i18n';
import type { CentralStock } from '@iplik/api-client/central-stock';
import { Badge, Group, Table, Text } from '@iplik/ui';
import { useState } from 'react';
import { formatSignedKg } from '@/modules/shipment';
import type { SortKey } from './StockFilters';
import { StockFilters } from './StockFilters';

/** Merkezde iplik cinsi başına eldeki kilo; ada göre arama, ada ya da kiloya göre sıralama. */
export function StockRows({ stock }: { stock: CentralStock }) {
  const { t } = useTranslation();
  const [search, setSearch] = useState('');
  const [sort, setSort] = useState<SortKey>('name');
  if (stock.rows.length === 0) return <Text c="dimmed">{t('stock.empty')}</Text>;
  const needle = search.trim().toLocaleLowerCase('tr');
  const rows = stock.rows
    .filter((row) => row.yarnType.toLocaleLowerCase('tr').includes(needle))
    .sort((a, b) =>
      sort === 'kg'
        ? Number(b.stockKg) - Number(a.stockKg)
        : a.yarnType.localeCompare(b.yarnType, 'tr'),
    );
  return (
    <>
      <StockFilters search={search} onSearch={setSearch} sort={sort} onSort={setSort} />
      <Table.ScrollContainer minWidth={360}>
        <Table striped aria-label={t('stock.title')}>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>{t('stock.yarnType')}</Table.Th>
              <Table.Th>{t('stock.stockKg')}</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {rows.map((row) => (
              <Table.Tr key={row.yarnType}>
                <Table.Td>{row.yarnType}</Table.Td>
                <Table.Td>
                  <Group gap="xs">
                    {formatSignedKg(row.stockKg)}
                    {row.stockKg.startsWith('-') && (
                      <Badge color="red" variant="light">
                        {t('stock.negativeBadge')}
                      </Badge>
                    )}
                  </Group>
                </Table.Td>
              </Table.Tr>
            ))}
            <Table.Tr>
              <Table.Th>{t('stock.total')}</Table.Th>
              <Table.Th>{formatSignedKg(stock.totalKg)}</Table.Th>
            </Table.Tr>
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
      {rows.length === 0 && <Text c="dimmed">{t('stock.noMatch')}</Text>}
    </>
  );
}
