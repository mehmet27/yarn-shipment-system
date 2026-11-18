import { useTranslation } from '@iplik/i18n';
import type { SupplierReceipt } from '@iplik/api-client/central-stock';
import { sumKg } from '@iplik/units';
import { Paper, Table, Text } from '@iplik/ui';
import { formatKilograms } from '../format';

type Group = { name: string; rows: SupplierReceipt[]; totalKg: string };

/** Aynı tedarikçinin satırlarını birleştirir; tedarikçi sırası gelen sıradır (sunucu ada göre sıralar). */
function groupBySupplier(rows: readonly SupplierReceipt[]): Group[] {
  const groups = new Map<string, SupplierReceipt[]>();
  for (const row of rows) {
    groups.set(row.supplierName, [...(groups.get(row.supplierName) ?? []), row]);
  }
  return [...groups].map(([name, own]) => ({
    name,
    rows: own,
    totalKg: sumKg(own.map((row) => row.receivedKg)),
  }));
}

/** Hangi tedarikçiden hangi iplikten ne kadar geldi: tedarikçi başlığında toplam, altında iplik cinsleri. */
export function SupplierReceipts({ suppliers }: { suppliers: readonly SupplierReceipt[] }) {
  const { t } = useTranslation();
  if (suppliers.length === 0) return <Text c="dimmed">{t('stock.suppliers.empty')}</Text>;
  return (
    <Paper withBorder style={{ overflow: 'hidden' }}>
      <Table.ScrollContainer minWidth={360}>
        <Table aria-label={t('stock.suppliers.title')}>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>{t('stock.suppliers.supplier')}</Table.Th>
              <Table.Th ta="right">{t('stock.suppliers.receivedKg')}</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {groupBySupplier(suppliers).flatMap((group) => [
              <Table.Tr key={group.name}>
                <Table.Th>{group.name}</Table.Th>
                <Table.Th ta="right">{formatKilograms(group.totalKg)}</Table.Th>
              </Table.Tr>,
              ...group.rows.map((row) => (
                <Table.Tr key={`${group.name}/${row.productId ?? row.yarnType}`}>
                  <Table.Td pl="xl">{row.yarnType}</Table.Td>
                  <Table.Td ta="right">{formatKilograms(row.receivedKg)}</Table.Td>
                </Table.Tr>
              )),
            ])}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    </Paper>
  );
}
