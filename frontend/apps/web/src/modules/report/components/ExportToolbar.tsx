import type { GetDashboardParams } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Icon, Switch } from '@iplik/ui';
import type { ReactNode } from 'react';
import { useReportExport } from '../hooks/useReportExport';

export type ExportToolbarProps = {
  params: GetDashboardParams;
  isOnlyDiscrepant: boolean;
  onIsOnlyDiscrepantChange: (value: boolean) => void;
  isDisabled: boolean;
  logo: string | null;
  /** Süzgeç satırının solunda, anahtarın yanında duran ek denetim (rapor logosu). */
  children?: ReactNode;
};

/** Excel ve PDF indirme düğmeleri; ekrandaki dönem ve süzgeç tüm sayfalara uygulanır. */
export function ExportToolbar({
  params,
  isOnlyDiscrepant,
  onIsOnlyDiscrepantChange,
  isDisabled,
  logo,
  children,
}: ExportToolbarProps) {
  const { t } = useTranslation();
  const { busy, hasFailed, run } = useReportExport(params, isOnlyDiscrepant, logo);
  return (
    <>
      <Group justify="space-between" gap="sm">
        <Group gap="md">
          <Switch
            label={t('report.export.onlyDiscrepant')}
            checked={isOnlyDiscrepant}
            onChange={(event) => {
              onIsOnlyDiscrepantChange(event.currentTarget.checked);
            }}
          />
          {children}
        </Group>
        <Group gap="xs">
          <Button
            variant="default"
            leftSection={<Icon name="excel" />}
            loading={busy === 'xlsx'}
            disabled={isDisabled || busy !== null}
            onClick={() => void run('xlsx')}
          >
            {t('report.export.excel')}
          </Button>
          <Button
            variant="default"
            leftSection={<Icon name="pdf" />}
            loading={busy === 'pdf'}
            disabled={isDisabled || busy !== null}
            onClick={() => void run('pdf')}
          >
            {t('report.export.pdf')}
          </Button>
        </Group>
      </Group>
      {hasFailed && (
        <Alert color="red" role="alert">
          {t('report.export.failed')}
        </Alert>
      )}
    </>
  );
}
