import type { ProblemKind } from '@iplik/api-client';
import { classifyProblem, toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Center, Loader } from '@iplik/ui';
import type { UseQueryResult } from '@tanstack/react-query';
import type { ReactNode } from 'react';

export type QueryStateProps<T> = {
  query: UseQueryResult<T>;
  children: (data: T) => ReactNode;
};

const BAD_REQUEST = 400;

function errorMessageKey(
  kind: ProblemKind,
  status: number,
): 'notFound' | 'forbidden' | 'unexpected' {
  if (status === BAD_REQUEST || kind === 'notFound') return 'notFound';
  return kind === 'forbidden' ? kind : 'unexpected';
}

/** FE-CPLX-08: yükleniyor / hata / veri durumları tek yerde, sırayla. */
export function QueryState<T>({ query, children }: QueryStateProps<T>) {
  const { t } = useTranslation();
  if (query.isPending) {
    return (
      <Center py="xl">
        <Loader />
      </Center>
    );
  }
  if (query.isError) {
    const problem = toProblem(query.error);
    const kind = classifyProblem(problem);
    // Geçersiz biçimli kimlik (400) kullanıcı için "bulunamadı" ile aynıdır.
    const messageKey = errorMessageKey(kind, problem.status);
    return <Alert color="red">{t(`error.${messageKey}`)}</Alert>;
  }
  return children(query.data);
}
