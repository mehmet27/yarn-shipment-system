import { useCurrentUser } from '@/modules/auth';
import { Navigate } from 'react-router';

/**
 * Fasoncu kapsamındaki dış kullanıcı mobil kabuğa, diğerleri panele gider. Karar rol adına değil
 * oturumun kapsamına göre verilir (FE-SEC-02); kullanıcı RequireAuth içinde zaten yüklenmiştir.
 * Sevkiyat izni olmayan salt okuma kullanıcısı (genel müdür) doğrudan raporlara iner.
 */
export function HomeRedirectPage() {
  const { data: user } = useCurrentUser();
  if (user?.subcontractorId !== undefined) return <Navigate to="/saha" replace />;
  const isReportOnly =
    user !== undefined &&
    !user.permissions.includes('SHIPMENT_VIEW') &&
    user.permissions.includes('REPORT_VIEW');
  return <Navigate to={isReportOnly ? '/panel/raporlar' : '/panel'} replace />;
}
