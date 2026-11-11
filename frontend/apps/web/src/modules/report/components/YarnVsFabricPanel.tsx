import { useTranslation } from '@iplik/i18n';
import { Stack, Table, Text } from '@iplik/ui';
import { useState } from 'react';
import { QueryState } from '@/modules/shipment';
import { yarnVsFabricTable } from '../export/faconTables';
import { formatFireRate, formatKg3 } from '../format';
import { useYarnVsFabricReport } from '../hooks/useFaconReports';
import { PanelExportBar } from './PanelExportBar';
import type { ReportPanelProps } from './reportPanelProps';
import { optionsOf } from './subcontractorOptions';

/** Fasoncu bazında giden iplik ↔ gelen kumaş karşılaştırması. */
export function YarnVsFabricPanel({ params, isEnabled }: ReportPanelProps) {
  const { t } = useTranslation();
  const query = useYarnVsFabricReport(params, isEnabled);
  const [subcontractorId, setSubcontractorId] = useState<string | null>(null);
  return (
    <QueryState query={query}>
      {(all) => {
        const rows =
          subcontractorId === null
            ? all
            : all.filter((row) => row.subcontractorId === subcontractorId);
        return (
          <Stack>
            <PanelExportBar
              tables={[yarnVsFabricTable(rows, t)]}
              fileName="iplik-kumas.xlsx"
              options={optionsOf(all)}
              subcontractorId={subcontractorId}
              onSubcontractorChange={setSubcontractorId}
            />
            {rows.length === 0 ? (
              <Text c="dimmed">{t('report.fire.empty')}</Text>
            ) : (
              <Table.ScrollContainer minWidth={640}>
                <Table striped>
                  <Table.Thead>
                    <Table.Tr>
                      <Table.Th>{t('report.parties.subcontractor')}</Table.Th>
                      <Table.Th>{t('report.fire.sent')}</Table.Th>
                      <Table.Th>{t('report.fire.returnedYarn')}</Table.Th>
                      <Table.Th>{t('report.fire.fabric')}</Table.Th>
                      <Table.Th>{t('report.fire.actualFireRate')}</Table.Th>
                      <Table.Th>{t('report.fire.shortage')}</Table.Th>
                    </Table.Tr>
                  </Table.Thead>
                  <Table.Tbody>
                    {rows.map((row) => (
                      <Table.Tr key={row.subcontractorId}>
                        <Table.Td>{row.subcontractorName}</Table.Td>
                        <Table.Td>{formatKg3(row.sentKg)}</Table.Td>
                        <Table.Td>{formatKg3(row.returnedYarnKg)}</Table.Td>
                        <Table.Td>{formatKg3(row.fabricKg)}</Table.Td>
                        <Table.Td>{formatFireRate(row.actualFireRate)}</Table.Td>
                        <Table.Td>{formatKg3(row.shortageKg)}</Table.Td>
                      </Table.Tr>
                    ))}
                  </Table.Tbody>
                </Table>
              </Table.ScrollContainer>
            )}
          </Stack>
        );
      }}
    </QueryState>
  );
}
