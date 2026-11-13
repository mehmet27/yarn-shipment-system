import { CHANGE_PASSWORD_PATH, classifyProblem, loginUrl, toProblem } from '@iplik/api-client';
import { Center, Loader } from '@iplik/ui';
import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router';
import { useCurrentUser } from '../hooks/useCurrentUser';

export type RequireAuthProps = {
  children: ReactNode;
  errorFallback: ReactNode;
  /** Şifre değiştirme sayfası için: geçici şifreyle girenin de görebileceği tek yer. */
  allowPasswordChangeRequired?: boolean;
};

/**
 * Oturum yoksa giriş sayfasına yönlendirir. Geçici şifreyle girenin şifresini değiştirmeden başka sayfaya geçmesine
 * izin vermez (sunucu da böyle bir kişiye hiçbir izin vermez).
 */
export function RequireAuth({
  children,
  errorFallback,
  allowPasswordChangeRequired = false,
}: RequireAuthProps) {
  const query = useCurrentUser();
  const location = useLocation();
  const isUnauthenticated =
    query.isError && classifyProblem(toProblem(query.error)) === 'unauthenticated';

  if (isUnauthenticated) {
    return <Navigate to={loginUrl(location.pathname + location.search)} replace />;
  }
  if (query.isSuccess) {
    if (query.data.passwordChangeRequired === true && !allowPasswordChangeRequired) {
      return <Navigate to={CHANGE_PASSWORD_PATH} replace />;
    }
    return children;
  }
  if (query.isError) return errorFallback;
  return (
    <Center h="100vh">
      <Loader />
    </Center>
  );
}
