import type { GetDashboardParams } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Switch } from '@iplik/ui';
import { useReportExport } from '../hooks/useReportExport';
import { PrintableReport } from './PrintableReport';

export type ExportToolbarProps = {
  params: GetDashboardParams;
  isOnlyDiscrepant: boolean;
  onIsOnlyDiscrepantChange: (value: boolean) => void;
  isDisabled: boolean;
  logo: string | null;
};

/** Excel indirme ve PDF (yazdır) düğmeleri; ekrandaki dönem ve süzgeç tüm sayfalara uygulanır. */
export function ExportToolbar({
  params,
  isOnlyDiscrepant,
  onIsOnlyDiscrepantChange,
  isDisabled,
  logo,
}: ExportToolbarProps) {
  const { t } = useTranslation();
  const { busy, hasFailed, printTables, run } = useReportExport(params, isOnlyDiscrepant);
  return (
    <>
      <Group>
        <Switch
          label={t('report.export.onlyDiscrepant')}
          checked={isOnlyDiscrepant}
          onChange={(event) => {
            onIsOnlyDiscrepantChange(event.currentTarget.checked);
          }}
        />
        <Button
          variant="default"
          loading={busy === 'xlsx'}
          disabled={isDisabled || busy !== null}
          onClick={() => void run('xlsx')}
        >
          {t('report.export.excel')}
        </Button>
        <Button
          variant="default"
          loading={busy === 'pdf'}
          disabled={isDisabled || busy !== null}
          onClick={() => void run('pdf')}
        >
          {t('report.export.pdf')}
        </Button>
      </Group>
      {hasFailed && (
        <Alert color="red" role="alert">
          {t('report.export.failed')}
        </Alert>
      )}
      {printTables !== null && (
        <PrintableReport tables={printTables} title={t('report.title')} logo={logo} />
      )}
    </>
  );
}
