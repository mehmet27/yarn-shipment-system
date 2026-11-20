import type { GetDashboardParams } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { BarList, SectionCard } from '@iplik/ui';
import { usePermission } from '@/modules/auth';
import { formatKilograms } from '../format';
import { useInvoicedKg, useToInvoiceKg } from '../hooks/useInvoicingTotals';

const NO_VALUE = '—';

/** "Ne kadar kilo faturalanacak, ne kadarı faturalandı?": Faturalama ekranının iki sekmesinin toplamları. */
export function InvoicingSection({ params }: { params: GetDashboardParams }) {
  const { t } = useTranslation();
  const canView = usePermission('BALANCE_VIEW');
  const toInvoiceKg = useToInvoiceKg(canView).data;
  const invoicedKg = useInvoicedKg(params, canView).data;
  if (!canView) return null;
  const row = (key: 'toInvoice' | 'invoiced', kg: string | undefined) => ({
    key,
    label: t(`report.charts.invoicing.${key}`),
    value: Number(kg ?? 0),
    valueLabel: kg === undefined ? NO_VALUE : formatKilograms(kg),
    note: t(`report.charts.invoicing.${key}Note`),
  });
  return (
    <SectionCard
      title={t('report.charts.invoicing.title')}
      description={t('report.charts.invoicing.description')}
      icon="invoices"
      testId="section-invoicing"
    >
      <BarList
        label={t('report.charts.invoicing.title')}
        testId="chart-invoicing"
        items={[row('toInvoice', toInvoiceKg), row('invoiced', invoicedKg)]}
      />
    </SectionCard>
  );
}
