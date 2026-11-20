import { useTranslation } from '@iplik/i18n';
import { Paper, Stack, Text } from '@iplik/ui';
import { useState } from 'react';
import { QueryState } from '@/modules/shared';
import { yarnVsFabricTable } from '../export/faconTables';
import { useYarnVsFabricReport } from '../hooks/useFaconReports';
import { FireBars } from './FireBars';
import { PanelExportBar } from './PanelExportBar';
import type { ReportPanelProps } from './reportPanelProps';
import { optionsOf } from './subcontractorOptions';
import { YarnVsFabricTable } from './YarnVsFabricTable';

/** Fasoncu bazında giden iplik ↔ gelen kumaş karşılaştırması: üstte fire oranı çubukları, altında tablo. */
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
              <>
                <Paper withBorder p="md">
                  <FireBars rows={rows} testId="chart-fire-detail" />
                </Paper>
                <YarnVsFabricTable rows={rows} />
              </>
            )}
          </Stack>
        );
      }}
    </QueryState>
  );
}
