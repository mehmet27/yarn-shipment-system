import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Icon } from '@iplik/ui';
import { useState } from 'react';
import type { ReportTable } from '../export/reportModel';
import { downloadWorkbook } from '../export/writeWorkbook';

export type ListExportButtonProps = {
  fileName: string;
  /** Süzgece uyan bütün satırları (yalnızca görünen sayfayı değil) okuyup tabloya çevirir. */
  loadTable: () => Promise<ReportTable>;
  isDisabled?: boolean;
};

/** Liste ekranlarındaki "Excel'e al" düğmesi: ekrandaki süzgece uyan tüm kayıtlar tek sayfalık dosya olur. */
export function ListExportButton({
  fileName,
  loadTable,
  isDisabled = false,
}: ListExportButtonProps) {
  const { t } = useTranslation();
  const [isBusy, setIsBusy] = useState(false);
  const [hasFailed, setHasFailed] = useState(false);

  async function run() {
    setIsBusy(true);
    setHasFailed(false);
    try {
      await downloadWorkbook([await loadTable()], fileName);
    } catch {
      setHasFailed(true);
    } finally {
      setIsBusy(false);
    }
  }

  return (
    <>
      <Button
        variant="default"
        leftSection={<Icon name="excel" />}
        loading={isBusy}
        disabled={isDisabled}
        onClick={() => void run()}
      >
        {t('report.export.listExcel')}
      </Button>
      {hasFailed && (
        <Alert color="red" role="alert">
          {t('report.export.listFailed')}
        </Alert>
      )}
    </>
  );
}
