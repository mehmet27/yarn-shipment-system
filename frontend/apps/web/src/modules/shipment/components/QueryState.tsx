import { classifyProblem, toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Center, Loader } from '@iplik/ui';
import type { UseQueryResult } from '@tanstack/react-query';
import type { ReactNode } from 'react';

export type QueryStateProps<T> = {
  query: UseQueryResult<T>;
  children: (data: T) => ReactNode;
};

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
    const kind = classifyProblem(toProblem(query.error));
    const messageKey = kind === 'notFound' || kind === 'forbidden' ? kind : 'unexpected';
    return <Alert color="red">{t(`error.${messageKey}`)}</Alert>;
  }
  return children(query.data);
}
