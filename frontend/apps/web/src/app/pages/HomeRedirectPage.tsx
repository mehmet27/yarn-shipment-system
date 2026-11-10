import { useCurrentUser } from '@/modules/auth';
import { Navigate } from 'react-router';

/**
 * Fasoncu kapsamındaki dış kullanıcı mobil kabuğa, diğerleri panele gider. Karar rol adına değil
 * oturumun kapsamına göre verilir (FE-SEC-02); kullanıcı RequireAuth içinde zaten yüklenmiştir.
 */
export function HomeRedirectPage() {
  const { data: user } = useCurrentUser();
  return <Navigate to={user?.subcontractorId === undefined ? '/panel' : '/saha'} replace />;
}
