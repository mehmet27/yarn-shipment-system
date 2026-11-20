import { useTranslation } from '@iplik/i18n';
import { BarList, SectionCard, Text } from '@iplik/ui';
import { usePermission } from '@/modules/auth';
import { QueryState } from '@/modules/shipment';
import { foldOthers, toKgBars } from '../charts/barItems';
import { sumBySubcontractor } from '../charts/subcontractorYarn';
import { formatKilograms } from '../format';
import { useSubcontractorYarn } from '../hooks/useStockOverview';
import { isPositive } from './amountFormat';

/** "Hangi fasoncuda ne kadar ipliğim var?": Fasoncu bakiyesi ekranındaki bugünkü bakiyenin fasoncu toplamları. */
export function SubcontractorYarnSection() {
  const { t } = useTranslation();
  const canView = usePermission('BALANCE_VIEW');
  const query = useSubcontractorYarn(canView);
  if (!canView) return null;
  return (
    <SectionCard
      title={t('report.charts.subcontractorYarn.title')}
      description={t('report.charts.subcontractorYarn.description')}
      icon="balances"
      testId="section-subcontractor-yarn"
    >
      <QueryState query={query}>
        {(balances) => {
          const entries = sumBySubcontractor(balances).map((row) => ({
            key: row.subcontractorId,
            label: row.subcontractorTitle,
            kg: row.balanceKg,
            note: isPositive(row.inTransitKg)
              ? t('report.charts.subcontractorYarn.inTransit', {
                  kg: formatKilograms(row.inTransitKg),
                })
              : undefined,
          }));
          if (entries.length === 0) {
            return <Text c="dimmed">{t('report.charts.subcontractorYarn.empty')}</Text>;
          }
          const other = (count: number) => t('report.charts.subcontractorYarn.other', { count });
          return (
            <BarList
              label={t('report.charts.subcontractorYarn.title')}
              testId="chart-subcontractor-yarn"
              items={toKgBars(foldOthers(entries, other), formatKilograms)}
            />
          );
        }}
      </QueryState>
    </SectionCard>
  );
}
