import type { GetDashboardParams } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Stack, Title } from '@iplik/ui';
import { usePermission } from '@/modules/auth';
import { QueryState } from '@/modules/shipment';
import { useAmountSummary } from '../hooks/useFaconReports';
import { AmountCards } from './AmountCards';
import { AmountSubcontractorTable } from './AmountSubcontractorTable';

/** Tutar bilgisi: yalnızca müdür ve genel müdür görür. Tutarlar her satırın kendi birim fiyatıyla hesaplanır. */
export function AmountOverview({ params }: { params: GetDashboardParams }) {
  const { t } = useTranslation();
  const canView = usePermission('AMOUNT_VIEW');
  const query = useAmountSummary(params, canView);
  if (!canView) return null;
  return (
    <Stack data-testid="amount-overview">
      <Title order={2} size="h3">
        {t('report.amount.title')}
      </Title>
      <QueryState query={query}>
        {(summary) => (
          <Stack>
            <AmountCards summary={summary} />
            <Title order={3} size="h4">
              {t('report.amount.bySubcontractor')}
            </Title>
            <AmountSubcontractorTable summary={summary} />
          </Stack>
        )}
      </QueryState>
    </Stack>
  );
}
