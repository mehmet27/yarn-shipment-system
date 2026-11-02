import type { LotView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Stack, Text, Title, useDebouncedValue } from '@iplik/ui';
import { useState } from 'react';
import { LotForm } from '../components/LotForm';
import { LotResults } from '../components/LotResults';
import { RecordToolbar } from '../components/RecordToolbar';
import { SavedNotice, useSavedNotice } from '../components/SavedNotice';
import { useLots } from '../hooks/useLots';

const PAGE_SIZE = 20;
const SEARCH_DEBOUNCE_MS = 300;

/** `undefined` kapalı, `'new'` yeni parti, parti nesnesi düzenleme. */
type Editing = LotView | 'new' | undefined;

/** Kalite ve yönetici: parti açar, kilitsiz partiyi düzenler ve kalitesini kilitler (rapor Bölüm 10). */
export function LotsPage() {
  const { t } = useTranslation();
  const [search, setSearch] = useState('');
  const [q] = useDebouncedValue(search, SEARCH_DEBOUNCE_MS);
  const [page, setPage] = useState(1);
  const [isShowingInactive, setShowingInactive] = useState(false);
  const [editing, setEditing] = useState<Editing>();
  const notice = useSavedNotice();
  const query = useLots({ q, page: page - 1, size: PAGE_SIZE, isShowingInactive });
  return (
    <Stack>
      <Title order={1} size="h2">
        {t('lots.title')}
      </Title>
      <Text c="dimmed">{t('lots.intro')}</Text>
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
      <SavedNotice saved={notice.saved} onDismiss={notice.onDismiss} />
      <LotResults query={query} page={page} onPage={setPage} onEdit={setEditing} />
      {editing !== undefined && (
        <LotForm
          lot={editing === 'new' ? undefined : editing}
          onClose={() => {
            setEditing(undefined);
          }}
          onSaved={(isNew) => {
            setEditing(undefined);
            notice.show(isNew);
          }}
        />
      )}
    </Stack>
  );
}
