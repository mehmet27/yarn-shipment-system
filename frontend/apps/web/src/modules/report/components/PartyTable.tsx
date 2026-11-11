import { useTranslation } from '@iplik/i18n';
import { Table, Text } from '@iplik/ui';
import type { ReactNode } from 'react';
import { partyTotals } from '../export/reportModel';
import { formatPercent } from '../format';

export type PartyRow = {
  key: string;
  parties: ReactNode[];
  shipmentCount: number;
  discrepantShipmentCount: number;
  discrepancyRate: string;
};

export type PartyTableProps = {
  partyHeaders: string[];
  rows: PartyRow[];
};

/** Şoför, fasoncu ve eşleşme raporlarının ortak tablosu; en yüksek oran üstte gelir. */
export function PartyTable({ partyHeaders, rows }: PartyTableProps) {
  const { t } = useTranslation();
  const totals = partyTotals(rows);
  if (rows.length === 0) {
    return <Text c="dimmed">{t('report.parties.empty')}</Text>;
  }
  return (
    <Table.ScrollContainer minWidth={520}>
      <Table striped>
        <Table.Thead>
          <Table.Tr>
            {partyHeaders.map((header, index) => (
              <Table.Th key={`${String(index)}-${header}`}>{header}</Table.Th>
            ))}
            <Table.Th>{t('report.parties.shipments')}</Table.Th>
            <Table.Th>{t('report.parties.discrepant')}</Table.Th>
            <Table.Th>{t('report.parties.rate')}</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {rows.map((row) => (
            <Table.Tr key={row.key}>
              {row.parties.map((party, index) => (
                <Table.Td key={`${String(index)}-${partyHeaders[index] ?? ''}`}>{party}</Table.Td>
              ))}
              <Table.Td>{row.shipmentCount}</Table.Td>
              <Table.Td>{row.discrepantShipmentCount}</Table.Td>
              <Table.Td>{formatPercent(row.discrepancyRate)}</Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
        <Table.Tfoot>
          <Table.Tr fw={700} data-testid="party-total">
            <Table.Td colSpan={partyHeaders.length}>{t('report.export.total')}</Table.Td>
            <Table.Td>{totals.shipments}</Table.Td>
            <Table.Td>{totals.discrepant}</Table.Td>
            <Table.Td>{formatPercent(totals.rate.toFixed(2))}</Table.Td>
          </Table.Tr>
        </Table.Tfoot>
      </Table>
    </Table.ScrollContainer>
  );
}
