import { useCurrentUser, usePermission } from '@/modules/auth';

/** Hangi fasoncu sekmelerinin görüneceği; izinler backend matrisini yansıtır (asıl kontrol backend'dedir). */
export function useSettlementAccess() {
  const canBalance = usePermission('BALANCE_VIEW');
  const canRecordFabric = usePermission('FABRIC_RECEIPT_RECORD');
  const canRecordYarn = usePermission('YARN_RETURN_RECORD');
  const canEditRate = usePermission('FIRE_RATE_EDIT');
  const canMarkInvoice = usePermission('INVOICE_MARK');
  const { data: user } = useCurrentUser();
  /** Merkez kalite fasoncu kumaşını görmez. */
  const isFabricHidden = user?.workArea === 'CENTRAL';
  return {
    fabric: !isFabricHidden && (canRecordFabric || canBalance),
    yarn: canRecordYarn || canBalance,
    fire: canBalance || canEditRate || canMarkInvoice,
  };
}
