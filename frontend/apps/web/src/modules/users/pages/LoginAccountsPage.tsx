import { useTranslation } from '@iplik/i18n';
import {
  Button,
  PageHero,
  Stack,
  Text,
  TextInput,
  useDebouncedValue,
  useDisclosure,
} from '@iplik/ui';
import { useState } from 'react';
import { type AccountFilter, NO_FILTER } from '../accountFilter';
import { AccountFilters } from '../components/AccountFilters';
import { CreateAccountModal } from '../components/CreateAccountModal';
import { LoginAccountResults } from '../components/LoginAccountResults';
import { useLoginAccounts } from '../hooks/useLoginAccounts';

const PAGE_SIZE = 20;
const SEARCH_DEBOUNCE_MS = 300;

/** Yönetici: giriş hesabı olan tüm kullanıcıları görür, her rolden hesap açar, pasife alır, rolünü değiştirir ve yeni
 * geçici şifre verir; şifre listede hiçbir yerde gösterilmez. */
export function LoginAccountsPage() {
  const { t } = useTranslation();
  const [search, setSearch] = useState('');
  const [q] = useDebouncedValue(search, SEARCH_DEBOUNCE_MS);
  const [page, setPage] = useState(1);
  const [filter, setFilter] = useState<AccountFilter>(NO_FILTER);
  const [isCreating, { open, close }] = useDisclosure(false);
  const query = useLoginAccounts({ q, page: page - 1, size: PAGE_SIZE });
  return (
    <Stack>
      <PageHero
        title={t('users.title')}
        actions={
          <Button variant="white" color="brand" onClick={open}>
            {t('users.create.open')}
          </Button>
        }
      />
      <CreateAccountModal opened={isCreating} onClose={close} />
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
      <AccountFilters value={filter} onChange={setFilter} />
      <LoginAccountResults
        filter={filter}
        query={query}
        isSearching={q.trim() !== ''}
        page={page}
        onPage={setPage}
      />
    </Stack>
  );
}
