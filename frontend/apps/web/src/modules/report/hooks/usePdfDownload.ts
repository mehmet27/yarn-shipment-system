import { useTranslation } from '@iplik/i18n';
import type { ReportTable } from '../export/reportModel';
import { downloadPdf } from '../export/writePdf';

const SIGNATURES = ['prepared', 'checked', 'approved'] as const;

/** Tabloları, ekran dilindeki başlık ve imza metinleriyle gerçek PDF dosyası olarak indirir. */
export function usePdfDownload() {
  const { t } = useTranslation();
  return (tables: ReportTable[], fileName: string, logo: string | null = null) =>
    downloadPdf(
      tables,
      {
        title: t('report.title'),
        intro: t('report.intro'),
        empty: t('report.parties.empty'),
        signatures: SIGNATURES.map((key) => t(`report.export.signature.${key}`)),
        dateLine: t('report.export.signature.dateLine'),
        logo,
      },
      fileName,
    );
}
