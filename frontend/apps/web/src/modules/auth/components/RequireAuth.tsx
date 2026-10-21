import { classifyProblem, LOGIN_PATH, toProblem } from '@iplik/api-client';
import { Center, Loader } from '@iplik/ui';
import { useEffect } from 'react';
import type { ReactNode } from 'react';
import { useCurrentUser } from '../hooks/useCurrentUser';

export type RequireAuthProps = {
  children: ReactNode;
  errorFallback: ReactNode;
};

/** Oturum yoksa gateway'in Keycloak girişine yönlendirir; frontend giriş formu yazmaz. */
export function RequireAuth({ children, errorFallback }: RequireAuthProps) {
  const query = useCurrentUser();
  const isUnauthenticated =
    query.isError && classifyProblem(toProblem(query.error)) === 'unauthenticated';

  useEffect(() => {
    if (isUnauthenticated) {
      window.location.assign(LOGIN_PATH);
    }
  }, [isUnauthenticated]);

  if (query.isSuccess) return children;
  if (query.isError && !isUnauthenticated) return errorFallback;
  return (
    <Center h="100vh">
      <Loader />
    </Center>
  );
}
