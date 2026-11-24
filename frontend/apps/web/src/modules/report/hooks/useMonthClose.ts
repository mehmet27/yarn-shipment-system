import {
  getFireReport,
  getSubcontractorBalanceReport,
  getToInvoiceReport,
} from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { useState } from 'react';
import { useCurrentUser } from '@/modules/auth';
import { balanceTable } from '../export/extraTables';
import { fireTable } from '../export/faconTables';
import { downloadPdf } from '../export/writePdf';
import { monthName, monthPeriod, toInvoicePeriod } from '../monthClose';
import { usePartyNames } from './usePartyNames';

const SIGNATURES = ['prepared', 'checked', 'approved'] as const;

/**
 * Ay sonu kapanış çıktısı: seçilen ayın fasoncu bakiyesi, fire durumu ve ay sonunda faturası bekleyen dosyalar tek
 * PDF'te, imza alanlarıyla. Üç rapor da ekrandaki süzgeçten bağımsız, ayın tamamı için çekilir.
 */
export function useMonthClose(logo: string | null) {
  const { t } = useTranslation();
  const { nameOf } = usePartyNames();
  const { data: me } = useCurrentUser();
  const [isBusy, setIsBusy] = useState(false);
  const [hasFailed, setHasFailed] = useState(false);

  async function run(month: string) {
    setIsBusy(true);
    setHasFailed(false);
    try {
      const period = monthPeriod(month);
      const [balances, fire, toInvoice] = await Promise.all([
        getSubcontractorBalanceReport(period),
        getFireReport(period),
        getToInvoiceReport(toInvoicePeriod(month)),
      ]);
      const meta = {
        ...period,
        generatedAt: new Date(),
        generatedBy: me?.fullName ?? '',
        onlyDiscrepant: false,
        nameOf,
      };
      const translate = (key: string) => t(key);
      await downloadPdf(
        [
          balanceTable(balances, meta, translate),
          fireTable(fire, translate, { title: t('report.export.sheets.fire'), withStatus: true }),
          fireTable(toInvoice, translate, {
            title: t('report.export.sheets.toInvoice'),
            withStatus: false,
          }),
        ],
        {
          title: t('report.monthClose.pdfTitle', { month: monthName(month) }),
          intro: t('report.monthClose.pdfNote', { by: meta.generatedBy }),
          empty: t('report.parties.empty'),
          signatures: SIGNATURES.map((key) => t(`report.export.signature.${key}`)),
          dateLine: t('report.export.signature.dateLine'),
          logo,
        },
        `ay-sonu-kapanis-${month}.pdf`,
      );
    } catch {
      setHasFailed(true);
    } finally {
      setIsBusy(false);
    }
  }

  return { isBusy, hasFailed, run };
}
