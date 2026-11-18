import type { AmountSummaryView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { formatMoney } from '../format';
import { unpricedText } from './amountFormat';
import { StatCard } from './StatCard';
import { StatGrid } from './StatGrid';

/** Üç tutar kartı: fasoncudaki iplik değeri, faturalanacak tutar, yolda olan (henüz teslim edilmemiş) iplik tutarı. */
export function AmountCards({ summary }: { summary: AmountSummaryView }) {
  const { t } = useTranslation();
  const unpriced = (kg: string) => t('report.amount.unpriced', { kg });
  return (
    <StatGrid>
      <StatCard
        label={t('report.amount.subcontractorYarn')}
        value={formatMoney(summary.subcontractorYarnAmount)}
        hint={
          unpricedText(summary.subcontractorUnpricedKg, unpriced) ?? t('report.amount.eachOwnPrice')
        }
        testId="stat-amount-subcontractor-yarn"
      />
      <StatCard
        label={t('report.amount.toInvoice')}
        value={formatMoney(summary.toInvoiceAmount)}
        hint={
          unpricedText(summary.toInvoiceUnpricedKg, unpriced) ?? t('report.amount.eachOwnPrice')
        }
        testId="stat-amount-to-invoice"
      />
      <StatCard
        label={t('report.amount.shipped')}
        value={formatMoney(summary.shippedAmount)}
        hint={
          unpricedText(summary.shippedUnpricedKg, unpriced) ?? t('report.dashboard.selectedPeriod')
        }
        testId="stat-amount-shipped"
      />
    </StatGrid>
  );
}
