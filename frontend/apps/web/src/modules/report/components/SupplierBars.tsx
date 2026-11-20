import { useTranslation } from '@iplik/i18n';
import type { SupplierReceipt } from '@iplik/api-client/central-stock';
import { BarList } from '@iplik/ui';
import { foldOthers, toKgBars } from '../charts/barItems';
import { groupBySupplier } from '../charts/supplierGroups';
import { formatKilograms } from '../format';

/** "Hangi tedarikçiden ne kadar iplik aldım?": tedarikçi toplamları, en çok gelen üstte; tablodaki toplamlarla aynı. */
export function SupplierBars({ suppliers }: { suppliers: readonly SupplierReceipt[] }) {
  const { t } = useTranslation();
  if (suppliers.length === 0) return null;
  const entries = groupBySupplier(suppliers).map((group) => ({
    key: group.name,
    label: group.name,
    kg: group.totalKg,
  }));
  const other = (count: number) => t('report.charts.suppliers.other', { count });
  return (
    <BarList
      label={t('report.charts.suppliers.chartLabel')}
      testId="chart-suppliers"
      items={toKgBars(foldOthers(entries, other), formatKilograms)}
    />
  );
}
