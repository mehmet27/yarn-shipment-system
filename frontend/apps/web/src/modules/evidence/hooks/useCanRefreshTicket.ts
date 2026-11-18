import { useCurrentUser } from '@/modules/auth';
import { UPLOAD_PERMISSION_BY_TYPE } from '../uploadTypes';
import { useShipmentEvidence } from './useEvidenceQueries';

/**
 * Kantar fişi yenileme bölümü görünür mü: yükleme izni ve çıkış tartısı gerekir. Fiş varsa sevkiyat yola çıkmamış
 * olmalıdır; fiş hiç yüklenememişse yolda olsa da yeniden denenebilir.
 */
export function useCanRefreshTicket(
  shipmentId: string,
  isDispatched: boolean,
  isDeparted: boolean,
) {
  const user = useCurrentUser();
  const evidence = useShipmentEvidence(shipmentId);
  const canUpload =
    user.data?.permissions.includes(UPLOAD_PERMISSION_BY_TYPE.DISPATCH_SCALE) ?? false;
  if (!isDispatched || !canUpload || evidence.data === undefined) return false;
  const hasTicket = evidence.data.some((item) => item.evidenceType === 'DISPATCH_SCALE');
  return !(hasTicket && isDeparted);
}
