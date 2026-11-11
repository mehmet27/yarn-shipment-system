import { useState } from 'react';
import type { ReportTable } from '../export/reportModel';
import { downloadWorkbook } from '../export/writeWorkbook';
import { usePdfDownload } from './usePdfDownload';

export type PanelExportFormat = 'xlsx' | 'pdf';

/** Tek panelin tablosunu Excel ya da PDF dosyası olarak indirir; dosya adı Excel adından türer. */
export function usePanelExport(tables: ReportTable[], fileName: string) {
  const [hasFailed, setHasFailed] = useState(false);
  const downloadPdf = usePdfDownload();

  async function run(format: PanelExportFormat) {
    setHasFailed(false);
    try {
      if (format === 'xlsx') await downloadWorkbook(tables, fileName);
      else await downloadPdf(tables, fileName.replace(/\.xlsx$/, '.pdf'));
    } catch {
      setHasFailed(true);
    }
  }
  return { hasFailed, run };
}
