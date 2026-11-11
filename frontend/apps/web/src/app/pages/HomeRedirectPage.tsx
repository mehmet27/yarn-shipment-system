import { useCurrentUser } from '@/modules/auth';
import { Navigate } from 'react-router';

/**
 * Tüm kullanıcılar fabrika personelidir (KR-17). Sevkiyat izni olmayan salt okuma kullanıcısı (genel müdür)
 * doğrudan raporlara, diğerleri panele gider (FE-SEC-02).
 */
export function HomeRedirectPage() {
  const { data: user } = useCurrentUser();
  const isReportOnly =
    user !== undefined &&
    !user.permissions.includes('SHIPMENT_VIEW') &&
    user.permissions.includes('REPORT_VIEW');
  return <Navigate to={isReportOnly ? '/panel/raporlar' : '/panel'} replace />;
}
