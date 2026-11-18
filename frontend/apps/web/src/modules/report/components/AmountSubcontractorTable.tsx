import type { AmountSummaryView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Table, Text } from '@iplik/ui';
import { formatKg3, formatMoney } from '../format';
import { isPositive } from './amountFormat';

/** Fasoncu bazında kalan iplik ve değeri. */
export function AmountSubcontractorTable({ summary }: { summary: AmountSummaryView }) {
  const { t } = useTranslation();
  if (summary.subcontractors.length === 0) {
    return (
      <Text c="dimmed" p="md">
        {t('report.amount.empty')}
      </Text>
    );
  }
  return (
    <Table.ScrollContainer minWidth={420}>
      <Table data-testid="amount-subcontractor-table">
        <Table.Thead>
          <Table.Tr>
            <Table.Th>{t('report.parties.subcontractor')}</Table.Th>
            <Table.Th ta="right">{t('report.amount.remainingKg')}</Table.Th>
            <Table.Th ta="right">{t('report.amount.value')}</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {summary.subcontractors.map((row) => (
            <Table.Tr key={row.subcontractorId}>
              <Table.Td>{row.subcontractorName}</Table.Td>
              <Table.Td ta="right">{formatKg3(row.balanceKg)}</Table.Td>
              <Table.Td ta="right">
                {formatMoney(row.amount)}
                {isPositive(row.unpricedKg) ? (
                  <Text size="xs" c="dimmed">
                    {t('report.amount.unpriced', { kg: formatKg3(row.unpricedKg) })}
                  </Text>
                ) : null}
              </Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  );
}
