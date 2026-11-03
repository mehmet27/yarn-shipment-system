import { useTranslation } from '@iplik/i18n';
import { Stack, Text, TextInput, Title, useDebouncedValue } from '@iplik/ui';
import { useState } from 'react';
import { LoginAccountResults } from '../components/LoginAccountResults';
import { useLoginAccounts } from '../hooks/useLoginAccounts';

const PAGE_SIZE = 20;
const SEARCH_DEBOUNCE_MS = 300;

/** Yönetici: giriş hesabı olan kullanıcıları kullanıcı adı, rol ve durumla görür; şifre gösterilmez. */
export function LoginAccountsPage() {
  const { t } = useTranslation();
  const [search, setSearch] = useState('');
  const [q] = useDebouncedValue(search, SEARCH_DEBOUNCE_MS);
  const [page, setPage] = useState(1);
  const query = useLoginAccounts({ q, page: page - 1, size: PAGE_SIZE });
  return (
    <Stack>
      <Title order={1} size="h2">
        {t('users.title')}
      </Title>
      <Text c="dimmed">{t('users.intro')}</Text>
      <TextInput
        type="search"
        aria-label={t('users.search')}
        placeholder={t('users.search')}
        value={search}
        onChange={(event) => {
          setSearch(event.currentTarget.value);
          setPage(1);
        }}
      />
      <LoginAccountResults
        query={query}
        isSearching={q.trim() !== ''}
        page={page}
        onPage={setPage}
      />
    </Stack>
  );
}
