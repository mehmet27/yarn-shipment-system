import type { LotView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { EmptyState, Group, Stack } from '@iplik/ui';
import { ListPagination, QueryState } from '@/modules/shared';
import type { useLots } from '../hooks/useLots';
import { LotCard } from './LotCard';

export type LotResultsProps = {
  query: ReturnType<typeof useLots>;
  page: number;
  onPage: (page: number) => void;
  onEdit: (lot: LotView) => void;
};

/** Parti kartları ve sayfalama; boş listede kısa bir bilgi. */
export function LotResults({ query, page, onPage, onEdit }: LotResultsProps) {
  const { t } = useTranslation();
  return (
    <QueryState query={query}>
      {({ rows, totalPages }) =>
        rows.length === 0 ? (
          <EmptyState title={t('masterData.empty')} />
        ) : (
          <Stack>
            {rows.map((lot) => (
              <LotCard key={lot.id} lot={lot} onEdit={onEdit} />
            ))}
            <Group justify="center">
              <ListPagination total={totalPages} value={page} onChange={onPage} />
            </Group>
          </Stack>
        )
      }
    </QueryState>
  );
}
