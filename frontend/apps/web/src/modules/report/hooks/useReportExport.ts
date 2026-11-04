import type { GetDashboardParams } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { useCurrentUser } from '@/modules/auth';
import { buildTables } from '../export/buildTables';
import type { ReportTable } from '../export/reportModel';
import { downloadWorkbook, reportFileName } from '../export/writeWorkbook';
import { usePartyNames } from './usePartyNames';
import { reportBundleQuery } from './reportBundleQuery';

export type ExportFormat = 'xlsx' | 'pdf';

/** Dışa aktarma akışı: tüm raporları çeker, Excel'i indirir ya da yazdırma bölümünü hazırlayıp yazdırmayı açar. */
export function useReportExport(params: GetDashboardParams, isOnlyDiscrepant: boolean) {
  const { nameOf } = usePartyNames();
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const { data: me } = useCurrentUser();
  const [busy, setBusy] = useState<ExportFormat | null>(null);
  const [hasFailed, setHasFailed] = useState(false);
  const [printTables, setPrintTables] = useState<ReportTable[] | null>(null);

  useEffect(() => {
    if (printTables === null) return;
    window.print();
    setPrintTables(null);
  }, [printTables]);

  async function run(format: ExportFormat) {
    setBusy(format);
    setHasFailed(false);
    try {
      const bundle = await queryClient.query(reportBundleQuery(params));
      const tables = buildTables(
        bundle,
        {
          from: bundle.from,
          to: bundle.to,
          generatedAt: new Date(),
          generatedBy: me?.fullName ?? '',
          onlyDiscrepant: isOnlyDiscrepant,
          nameOf,
        },
        (key) => t(key),
      );
      if (format === 'xlsx') {
        await downloadWorkbook(tables, reportFileName(bundle.from, bundle.to, 'xlsx'));
      } else {
        setPrintTables(tables);
      }
    } catch {
      setHasFailed(true);
    } finally {
      setBusy(null);
    }
  }

  return { busy, hasFailed, printTables, run };
}
