import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Icon } from '@iplik/ui';
import { downloadWorkbook } from '@/modules/report';
import { useState } from 'react';
import type { InvoicedFilter } from '../hooks/useFireSettlements';
import { fetchAllInvoiced } from '../hooks/useFireSettlements';
import { invoicedTable } from './invoicedTable';

/** "Excel'e al": süzgece uyan tüm faturalar (yalnız görünen sayfa değil) tek sayfalık dosya olur. */
export function InvoicedExport({
  filter,
  totalKg,
  isDisabled,
}: {
  filter: InvoicedFilter;
  totalKg: string;
  isDisabled: boolean;
}) {
  const { t } = useTranslation();
  const [isBusy, setIsBusy] = useState(false);
  const [hasFailed, setHasFailed] = useState(false);

  async function run() {
    setIsBusy(true);
    setHasFailed(false);
    try {
      const rows = await fetchAllInvoiced(filter);
      await downloadWorkbook([invoicedTable(rows, totalKg, t)], 'faturalananlar.xlsx');
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
        {t('settlement.invoiced.excel')}
      </Button>
      {hasFailed && (
        <Alert color="red" role="alert">
          {t('settlement.invoiced.exportFailed')}
        </Alert>
      )}
    </>
  );
}
