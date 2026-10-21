import { usePermission } from '@/modules/auth';
import { Navigate } from 'react-router';

/**
 * Saha işi yapan kullanıcı (şoför beyanı, kör teslim girişi) mobil kabuğa, diğerleri panele gider.
 * Karar rol adına değil izne göre verilir (FE-SEC-02).
 */
export function HomeRedirectPage() {
  const isDriver = usePermission('DRIVER_DECLARATION_RECORD');
  const isSubcontractor = usePermission('DELIVERY_RECORD');
  return <Navigate to={isDriver || isSubcontractor ? '/saha' : '/panel'} replace />;
}
