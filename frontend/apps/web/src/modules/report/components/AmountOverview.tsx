import type { GetDashboardParams } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { SectionCard } from '@iplik/ui';
import { usePermission } from '@/modules/auth';
import { QueryState } from '@/modules/shipment';
import { useAmountSummary } from '../hooks/useFaconReports';
import { AmountCards } from './AmountCards';
import { AmountSubcontractorTable } from './AmountSubcontractorTable';
import { CardBleed } from './CardBleed';
import { GroupLabel } from './GroupLabel';

/** Tutar bilgisi: yalnızca müdür ve genel müdür görür. Tutarlar her satırın kendi birim fiyatıyla hesaplanır. */
export function AmountOverview({ params }: { params: GetDashboardParams }) {
  const { t } = useTranslation();
  const canView = usePermission('AMOUNT_VIEW');
  const query = useAmountSummary(params, canView);
  if (!canView) return null;
  return (
    <SectionCard title={t('report.amount.title')} icon="invoices" testId="amount-overview">
      <QueryState query={query}>
        {(summary) => (
          <CardBleed>
            <AmountCards summary={summary} />
            <GroupLabel>{t('report.amount.bySubcontractor')}</GroupLabel>
            <AmountSubcontractorTable summary={summary} />
          </CardBleed>
        )}
      </QueryState>
    </SectionCard>
  );
}
