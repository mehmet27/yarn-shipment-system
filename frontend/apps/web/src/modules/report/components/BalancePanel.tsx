import { useTranslation } from '@iplik/i18n';
import { Stack, Table, Text } from '@iplik/ui';
import { QueryState } from '@/modules/shipment';
import { formatKilograms } from '../format';
import { usePartyNames } from '../hooks/usePartyNames';
import { useBalanceReport } from '../hooks/usePartyReport';
import type { ReportPanelProps } from './reportPanelProps';

/** Fasoncu bazında gönderilen, teslim alınan, yolda kalan kilo ve fire. */
export function BalancePanel({ params, isEnabled }: ReportPanelProps) {
  const { t } = useTranslation();
  const { nameOf } = usePartyNames();
  const query = useBalanceReport(params, isEnabled);
  return (
    <QueryState query={query}>
      {(rows) =>
        rows.length === 0 ? (
          <Text c="dimmed">{t('report.shipments.empty')}</Text>
        ) : (
          <Stack>
            <Text size="sm" c="dimmed">
              {t('report.balance.note')}
            </Text>
            <Table.ScrollContainer minWidth={560}>
              <Table striped>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>{t('report.parties.subcontractor')}</Table.Th>
                    <Table.Th>{t('report.parties.shipments')}</Table.Th>
                    <Table.Th>{t('report.export.dispatched')}</Table.Th>
                    <Table.Th>{t('report.export.delivered')}</Table.Th>
                    <Table.Th>{t('report.export.inTransit')}</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {rows.map((row) => (
                    <Table.Tr key={row.subcontractorId}>
                      <Table.Td>{nameOf(row.subcontractorId)}</Table.Td>
                      <Table.Td>{row.shipmentCount}</Table.Td>
                      <Table.Td>{formatKilograms(row.dispatchedKg)}</Table.Td>
                      <Table.Td>{formatKilograms(row.deliveredKg)}</Table.Td>
                      <Table.Td>{formatKilograms(row.inTransitKg)}</Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
          </Stack>
        )
      }
    </QueryState>
  );
}
