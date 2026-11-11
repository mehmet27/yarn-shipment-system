import { useEffect, useState } from 'react';
import type { ReportTable } from '../export/reportModel';
import { downloadWorkbook } from '../export/writeWorkbook';

export type PanelExportFormat = 'xlsx' | 'pdf';

/** Tek panelin tablosunu Excel olarak indirir ya da yazdırma bölümüne verir. */
export function usePanelExport(tables: ReportTable[], fileName: string) {
  const [printTables, setPrintTables] = useState<ReportTable[] | null>(null);
  const [hasFailed, setHasFailed] = useState(false);

  useEffect(() => {
    if (printTables === null) return;
    window.print();
    setPrintTables(null);
  }, [printTables]);

  async function run(format: PanelExportFormat) {
    setHasFailed(false);
    try {
      if (format === 'xlsx') await downloadWorkbook(tables, fileName);
      else setPrintTables(tables);
    } catch {
      setHasFailed(true);
    }
  }
  return { printTables, hasFailed, run };
}
