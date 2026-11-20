import type { MonthlyFlowView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Table } from '@iplik/ui';
import { formatMonth } from '../charts/trendRows';
import { formatFireRate, formatKg3 } from '../format';

const MIN_WIDTH = 560;

/** Eğilim grafiğinin rakamları; grafik ile aynı satırlardan üretilir. Sevkiyatı olmayan ayda fire oranı yazılmaz. */
export function TrendTable({ flows }: { flows: readonly MonthlyFlowView[] }) {
  const { t } = useTranslation();
  return (
    <Table.ScrollContainer minWidth={MIN_WIDTH}>
      <Table data-testid="trend-table">
        <Table.Thead>
          <Table.Tr>
            <Table.Th>{t('report.charts.trend.month')}</Table.Th>
            <Table.Th ta="right">{t('report.charts.trend.sent')}</Table.Th>
            <Table.Th ta="right">{t('report.fire.returnedYarn')}</Table.Th>
            <Table.Th ta="right">{t('report.charts.trend.fabric')}</Table.Th>
            <Table.Th ta="right">{t('report.fire.actualFireRate')}</Table.Th>
            <Table.Th ta="right">{t('report.charts.trend.shipments')}</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {flows.map((flow) => (
            <Table.Tr key={flow.month}>
              <Table.Td>{formatMonth(flow.month)}</Table.Td>
              <Table.Td ta="right">{formatKg3(flow.sentKg)}</Table.Td>
              <Table.Td ta="right">{formatKg3(flow.returnedYarnKg)}</Table.Td>
              <Table.Td ta="right">{formatKg3(flow.fabricKg)}</Table.Td>
              <Table.Td ta="right">
                {flow.shipmentCount === 0 ? '—' : formatFireRate(flow.actualFireRate)}
              </Table.Td>
              <Table.Td ta="right">{flow.shipmentCount}</Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  );
}
