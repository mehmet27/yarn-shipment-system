import { reportError } from '@iplik/ui';
import { useEffect } from 'react';
import { useRouteError } from 'react-router';
import { CrashPage } from './CrashPage';

/** Yönlendiricinin `errorElement`'i: yakalanan hatayı raporlar, sonra CrashPage'i gösterir. */
export function RouteErrorPage() {
  const error = useRouteError();
  useEffect(() => {
    reportError(error);
  }, [error]);
  return <CrashPage />;
}
