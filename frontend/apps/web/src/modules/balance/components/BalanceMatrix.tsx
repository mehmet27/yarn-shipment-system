import type { BalancesView } from '@iplik/api-client/settlements';
import { useTranslation } from '@iplik/i18n';
import { Table, Text } from '@iplik/ui';
import { sumKg } from '@iplik/units';
import { formatSignedKg } from '@/modules/shipment';

type Cells = Map<string, string>;

/** Satır: fasoncu, sütun: iplik cinsi; hücre bakiye kilosu. Toplamlar `sumKg` ile (Big). */
export function BalanceMatrix({ view }: { view: BalancesView }) {
  const { t } = useTranslation();
  if (view.items.length === 0) return <Text c="dimmed">{t('settlement.balances.empty')}</Text>;
  const yarns = [...new Set(view.items.map((row) => row.yarnType))].sort();
  const parties = new Map<string, { title: string; cells: Cells }>();
  for (const row of view.items) {
    const party = parties.get(row.subcontractorId) ?? {
      title: row.subcontractorTitle,
      cells: new Map(),
    };
    party.cells.set(row.yarnType, row.balanceKg);
    parties.set(row.subcontractorId, party);
  }
  return (
    <Table.ScrollContainer minWidth={480}>
      <Table striped aria-label={t('settlement.balances.title')}>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>{t('settlement.balances.subcontractor')}</Table.Th>
            {yarns.map((yarn) => (
              <Table.Th key={yarn}>{yarn}</Table.Th>
            ))}
            <Table.Th>{t('settlement.balances.total')}</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {[...parties.entries()].map(([id, party]) => (
            <Table.Tr key={id}>
              <Table.Td>{party.title}</Table.Td>
              {yarns.map((yarn) => (
                <Table.Td key={yarn}>{formatSignedKg(party.cells.get(yarn) ?? '0.000')}</Table.Td>
              ))}
              <Table.Td fw={600}>{formatSignedKg(sumKg([...party.cells.values()]))}</Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
        <Table.Tfoot>
          <Table.Tr>
            <Table.Th>{t('settlement.balances.total')}</Table.Th>
            {yarns.map((yarn) => (
              <Table.Th key={yarn}>
                {formatSignedKg(
                  sumKg(
                    view.items.filter((row) => row.yarnType === yarn).map((row) => row.balanceKg),
                  ),
                )}
              </Table.Th>
            ))}
            <Table.Th data-testid="balance-total">{formatSignedKg(view.totalKg)}</Table.Th>
          </Table.Tr>
        </Table.Tfoot>
      </Table>
    </Table.ScrollContainer>
  );
}
