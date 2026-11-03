import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Center, Group, Loader, Stack, Text } from '@iplik/ui';
import { ListPagination } from '@/modules/shipment';
import type { useLoginAccounts } from '../hooks/useLoginAccounts';
import { LoginAccountTable } from './LoginAccountTable';

export type LoginAccountResultsProps = {
  query: ReturnType<typeof useLoginAccounts>;
  isSearching: boolean;
  page: number;
  onPage: (page: number) => void;
};

/** Yükleniyor, hata (yeniden dene), boş ve dolu liste durumları (FE-CPLX-08). */
export function LoginAccountResults({
  query,
  isSearching,
  page,
  onPage,
}: LoginAccountResultsProps) {
  const { t } = useTranslation();
  if (query.isPending) {
    return (
      <Center py="xl">
        <Loader aria-label={t('app.loading')} />
      </Center>
    );
  }
  if (query.isError) {
    const isUnavailable = toProblem(query.error).code === 'LOGIN_ACCOUNT_UNAVAILABLE';
    return (
      <Alert color="red">
        <Stack gap="sm" align="flex-start">
          <Text size="sm">
            {t(isUnavailable ? 'users.errors.LOGIN_ACCOUNT_UNAVAILABLE' : 'users.errors.unknown')}
          </Text>
          <Button
            variant="default"
            onClick={() => {
              void query.refetch();
            }}
          >
            {t('error.retry')}
          </Button>
        </Stack>
      </Alert>
    );
  }
  const { rows, totalPages, totalElements } = query.data;
  if (rows.length === 0) {
    return <Text c="dimmed">{t(isSearching ? 'users.noMatch' : 'users.empty')}</Text>;
  }
  return (
    <Stack>
      <Text size="sm" c="dimmed">
        {t('users.count', { count: totalElements })}
      </Text>
      <LoginAccountTable accounts={rows} />
      {totalPages > 1 && (
        <Group justify="center">
          <ListPagination total={totalPages} value={page} onChange={onPage} />
        </Group>
      )}
    </Stack>
  );
}
