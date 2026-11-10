import { Navigate } from 'react-router';

/** Tüm kullanıcılar fabrika personelidir (KR-17); ana sayfa panele yönlenir. */
export function HomeRedirectPage() {
  return <Navigate to="/panel" replace />;
}
