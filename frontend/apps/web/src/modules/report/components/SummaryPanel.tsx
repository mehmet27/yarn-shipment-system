import { Stack } from '@iplik/ui';
import { QueryState } from '@/modules/shipment';
import { useDashboard } from '../hooks/useDashboard';
import type { ReportPanelProps } from './reportPanelProps';
import { AttentionLists } from './AttentionLists';
import { DashboardSummary } from './DashboardSummary';

/** Rapor ekranının "Özet" sekmesi: seçili dönemin dashboard verisi. */
export function SummaryPanel({ params, isEnabled }: ReportPanelProps) {
  const query = useDashboard(params);
  if (!isEnabled) return null;
  return (
    <QueryState query={query}>
      {(dashboard) => (
        <Stack>
          <DashboardSummary dashboard={dashboard} />
          <AttentionLists dashboard={dashboard} />
        </Stack>
      )}
    </QueryState>
  );
}
