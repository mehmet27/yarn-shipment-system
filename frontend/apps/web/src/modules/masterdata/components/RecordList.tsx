import { useTranslation } from '@iplik/i18n';
import { Group, Pagination, Stack, Text, useDebouncedValue } from '@iplik/ui';
import { useState } from 'react';
import { QueryState } from '@/modules/shipment';
import { useRecords } from '../hooks/useRecords';
import type { ManagedKind, RecordRow } from '../kinds';
import { RecordCard } from './RecordCard';
import { RecordForm } from './RecordForm';
import { RecordToolbar } from './RecordToolbar';

const PAGE_SIZE = 20;
const SEARCH_DEBOUNCE_MS = 300;

/** `undefined` kapalı, `'new'` yeni kayıt, kayıt nesnesi düzenleme. */
type Editing = RecordRow | 'new' | undefined;

export function RecordList({ kind }: { kind: ManagedKind }) {
  const { t } = useTranslation();
  const [search, setSearch] = useState('');
  const [q] = useDebouncedValue(search, SEARCH_DEBOUNCE_MS);
  const [page, setPage] = useState(1);
  const [isShowingInactive, setShowingInactive] = useState(false);
  const [editing, setEditing] = useState<Editing>();
  const query = useRecords(kind, { q, page: page - 1, size: PAGE_SIZE, isShowingInactive });
  return (
    <Stack>
      <RecordToolbar
        search={search}
        isShowingInactive={isShowingInactive}
        onAdd={() => {
          setEditing('new');
        }}
        onSearch={(next) => {
          setSearch(next);
          setPage(1);
        }}
        onToggleInactive={(isShowing) => {
          setShowingInactive(isShowing);
          setPage(1);
        }}
      />
      <QueryState query={query}>
        {({ rows, totalPages }) =>
          rows.length === 0 ? (
            <Text c="dimmed">{t('masterData.empty')}</Text>
          ) : (
            <Stack>
              {rows.map((record) => (
                <RecordCard key={record.id} kind={kind} record={record} onEdit={setEditing} />
              ))}
              <Group justify="center">
                <Pagination total={totalPages} value={page} onChange={setPage} />
              </Group>
            </Stack>
          )
        }
      </QueryState>
      {editing !== undefined && (
        <RecordForm
          kind={kind}
          record={editing === 'new' ? undefined : editing}
          onClose={() => {
            setEditing(undefined);
          }}
        />
      )}
    </Stack>
  );
}
