import { useTranslation } from '@iplik/i18n';
import { Stack, Table, Text } from '@iplik/ui';
import { QueryState } from '@/modules/shipment';
import { useShipmentReport } from '../hooks/usePartyReport';
import type { ReportPanelProps } from './reportPanelProps';
import { ShipmentRow } from './ShipmentRow';

/** Dönemdeki sevkiyatların satır satır dökümü. */
export function ShipmentsPanel({ params, isEnabled, isOnlyDiscrepant = false }: ReportPanelProps) {
  const { t } = useTranslation();
  const query = useShipmentReport(params, isEnabled);
  return (
    <QueryState query={query}>
      {(all) => {
        const rows = isOnlyDiscrepant ? all.filter((row) => row.hadDiscrepancy) : all;
        if (rows.length === 0) return <Text c="dimmed">{t('report.shipments.empty')}</Text>;
        return (
          <Stack>
            <Text size="sm" c="dimmed">
              {t('report.shipments.limitNote')}
            </Text>
            <Table.ScrollContainer minWidth={900}>
              <Table striped>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>{t('report.export.shipmentNo')}</Table.Th>
                    <Table.Th>{t('report.parties.subcontractor')}</Table.Th>
                    <Table.Th>{t('report.parties.driver')}</Table.Th>
                    <Table.Th>{t('report.export.status')}</Table.Th>
                    <Table.Th>{t('report.dashboard.dispatchNet')}</Table.Th>
                    <Table.Th>{t('report.dashboard.deliveryNet')}</Table.Th>
                    <Table.Th>{t('report.export.diffKg')}</Table.Th>
                    <Table.Th>{t('report.export.diffPct')}</Table.Th>
                    <Table.Th>{t('report.export.band')}</Table.Th>
                    <Table.Th>{t('report.export.createdAt')}</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {rows.map((row) => (
                    <ShipmentRow key={row.shipmentId} row={row} />
                  ))}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
          </Stack>
        );
      }}
    </QueryState>
  );
}
