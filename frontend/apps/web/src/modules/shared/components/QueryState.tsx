import type { ProblemKind } from '@iplik/api-client';
import { classifyProblem, toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Center, Loader, Stack, Text } from '@iplik/ui';
import type { UseQueryResult } from '@tanstack/react-query';
import type { ReactNode } from 'react';

export type QueryStateProps<T> = {
  query: UseQueryResult<T>;
  children: (data: T) => ReactNode;
};

type ErrorMessageKey = 'notFound' | 'forbidden' | 'unexpected';

const BAD_REQUEST = 400;

function errorMessageKey(kind: ProblemKind, status: number): ErrorMessageKey {
  if (status === BAD_REQUEST || kind === 'notFound') return 'notFound';
  return kind === 'forbidden' ? kind : 'unexpected';
}

/**
 * FE-CPLX-08: yükleniyor / hata / veri durumları tek yerde, sırayla. Yükleniyor durumu ekran okuyucuya
 * "Yükleniyor…" diye okunur; geçici hatada (bağlantı, sunucu) "Yeniden dene" sayfayı yenilemeden sorguyu tekrarlar.
 * Yetki ve "bulunamadı" hatasında yeniden denemek sonucu değiştirmez; düğme çıkmaz.
 */
export function QueryState<T>({ query, children }: QueryStateProps<T>) {
  const { t } = useTranslation();
  if (query.isPending) {
    return (
      <Center py="xl" role="status" aria-live="polite">
        <Loader aria-label={t('app.loading')} />
      </Center>
    );
  }
  if (query.isError) {
    const problem = toProblem(query.error);
    const kind = classifyProblem(problem);
    // Geçersiz biçimli kimlik (400) kullanıcı için "bulunamadı" ile aynıdır.
    const messageKey = errorMessageKey(kind, problem.status);
    return (
      <Alert color="red">
        <Stack gap="sm" align="flex-start">
          <Text size="sm">{t(`error.${messageKey}`)}</Text>
          {messageKey === 'unexpected' && (
            <Button
              variant="default"
              loading={query.isRefetching}
              onClick={() => {
                void query.refetch();
              }}
            >
              {t('error.retry')}
            </Button>
          )}
        </Stack>
      </Alert>
    );
  }
  return children(query.data);
}
