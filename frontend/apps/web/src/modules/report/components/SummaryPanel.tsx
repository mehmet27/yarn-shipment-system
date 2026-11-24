import { Stack } from '@iplik/ui';
import { QueryState } from '@/modules/shared';
import { useDashboard } from '../hooks/useDashboard';
import { usePreviousDashboard } from '../hooks/usePreviousDashboard';
import type { ReportPanelProps } from './reportPanelProps';
import { AttentionLists } from './AttentionLists';
import { CentralStockSection } from './CentralStockSection';
import { DashboardSummary } from './DashboardSummary';
import { FireBySubcontractorSection } from './FireBySubcontractorSection';
import { InvoicingSection } from './InvoicingSection';
import { StockOverview } from './StockOverview';
import { SubcontractorYarnSection } from './SubcontractorYarnSection';
import { TrendSection } from './TrendSection';

/**
 * Rapor ekranının "Özet" sekmesi, genel müdürün soru sırasıyla: karar bekleyenler, stok, aylık eğilim, fasoncularda
 * iplik, fire, faturalama, tutar, sevkiyatlar, merkez stok ve tedarikçiler. Her grafiğin rakamı yanında yazar.
 */
export function SummaryPanel({ params, isEnabled }: ReportPanelProps) {
  const query = useDashboard(params);
  const previous = usePreviousDashboard(params);
  if (!isEnabled) return null;
  return (
    <QueryState query={query}>
      {(dashboard) => (
        <Stack>
          <AttentionLists dashboard={dashboard} />
          <StockOverview params={params}>
            <TrendSection params={params} />
            <SubcontractorYarnSection />
            <FireBySubcontractorSection params={params} />
            <InvoicingSection params={params} />
          </StockOverview>
          <DashboardSummary dashboard={dashboard} previous={previous} />
          <CentralStockSection />
        </Stack>
      )}
    </QueryState>
  );
}
