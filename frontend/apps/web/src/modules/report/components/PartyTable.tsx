import { useTranslation } from '@iplik/i18n';
import { Table, Text } from '@iplik/ui';
import type { ReactNode } from 'react';
import { formatKilograms, formatPercent } from '../format';

export type PartyRow = {
  key: string;
  parties: ReactNode[];
  shipmentCount: number;
  discrepantShipmentCount: number;
  discrepancyRate: string;
  averageAbsoluteDiffKg: string;
};

export type PartyTableProps = {
  partyHeaders: string[];
  rows: PartyRow[];
};

/** Şoför, fasoncu ve eşleşme raporlarının ortak tablosu; en yüksek oran üstte gelir. */
export function PartyTable({ partyHeaders, rows }: PartyTableProps) {
  const { t } = useTranslation();
  if (rows.length === 0) {
    return <Text c="dimmed">{t('report.parties.empty')}</Text>;
  }
  return (
    <Table.ScrollContainer minWidth={520}>
      <Table striped>
        <Table.Thead>
          <Table.Tr>
            {partyHeaders.map((header) => (
              <Table.Th key={header}>{header}</Table.Th>
            ))}
            <Table.Th>{t('report.parties.shipments')}</Table.Th>
            <Table.Th>{t('report.parties.discrepant')}</Table.Th>
            <Table.Th>{t('report.parties.rate')}</Table.Th>
            <Table.Th>{t('report.parties.averageDiff')}</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {rows.map((row) => (
            <Table.Tr key={row.key}>
              {row.parties.map((party, index) => (
                <Table.Td key={partyHeaders[index]}>{party}</Table.Td>
              ))}
              <Table.Td>{row.shipmentCount}</Table.Td>
              <Table.Td>{row.discrepantShipmentCount}</Table.Td>
              <Table.Td>{formatPercent(row.discrepancyRate)}</Table.Td>
              <Table.Td>{formatKilograms(row.averageAbsoluteDiffKg)}</Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  );
}
