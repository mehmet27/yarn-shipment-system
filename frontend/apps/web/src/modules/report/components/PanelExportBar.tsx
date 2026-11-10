import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Select } from '@iplik/ui';
import { usePanelExport } from '../hooks/usePanelExport';
import type { ReportTable } from '../export/reportModel';
import { PrintableReport } from './PrintableReport';

export type SubcontractorOption = { value: string; label: string };

type PanelExportBarProps = {
  tables: ReportTable[];
  fileName: string;
  options?: SubcontractorOption[];
  subcontractorId?: string | null;
  onSubcontractorChange?: (value: string | null) => void;
};

/** Panel başına fasoncu süzgeci ile Excel ve PDF (yazdır) düğmeleri; dönem/lokasyon üst süzgeçten gelir. */
export function PanelExportBar({
  tables,
  fileName,
  options,
  subcontractorId,
  onSubcontractorChange,
}: PanelExportBarProps) {
  const { t } = useTranslation();
  const { printTables, hasFailed, run } = usePanelExport(tables, fileName);
  return (
    <>
      <Group>
        {options !== undefined && onSubcontractorChange !== undefined && (
          <Select
            aria-label={t('report.parties.subcontractor')}
            placeholder={t('report.fire.allSubcontractors')}
            data={options}
            value={subcontractorId ?? null}
            onChange={onSubcontractorChange}
            clearable
          />
        )}
        <Button variant="default" onClick={() => void run('xlsx')}>
          {t('report.export.excel')}
        </Button>
        <Button variant="default" onClick={() => void run('pdf')}>
          {t('report.export.pdf')}
        </Button>
      </Group>
      {hasFailed && (
        <Alert color="red" role="alert">
          {t('report.export.failed')}
        </Alert>
      )}
      {printTables !== null && <PrintableReport tables={printTables} title={t('report.title')} />}
    </>
  );
}
