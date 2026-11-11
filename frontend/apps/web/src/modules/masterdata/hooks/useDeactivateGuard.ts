import { getBalances } from '@iplik/api-client/settlements';
import type { BalanceRow } from '@iplik/api-client/settlements';
import { isZeroKg } from '@iplik/units';
import { useState } from 'react';
import type { ManagedKind, RecordRow } from '../kinds';
import { useSetActive } from './useSetActive';

/** KR-18: açık bakiyesi (ya da yolda ipliği) olan fasoncu için uyarı gerekir. */
export function openBalanceRows(items: readonly BalanceRow[]): BalanceRow[] {
  return items.filter((row) => !isZeroKg(row.balanceKg) || !isZeroKg(row.inTransitKg));
}

export type PendingWarning = { record: RecordRow; rows: BalanceRow[] | undefined };

/**
 * Pasife alma düğmesinin arkasındaki kontrol. Fasoncuda önce bakiye sorulur; bakiye varsa ya da
 * sorgulanamadıysa (`rows` boş) onay penceresi açılır, yoksa doğrudan pasife alınır.
 */
export function useDeactivateGuard(kind: ManagedKind) {
  const mutation = useSetActive(kind);
  const [warning, setWarning] = useState<PendingWarning | undefined>();
  const [isChecking, setChecking] = useState(false);

  const request = async (record: RecordRow) => {
    if (kind !== 'subcontractor' || !record.isActive) {
      mutation.mutate({ id: record.id, isActive: !record.isActive });
      return;
    }
    setChecking(true);
    try {
      const rows = openBalanceRows((await getBalances(record.id)).items);
      if (rows.length === 0) {
        mutation.mutate({ id: record.id, isActive: false });
      } else {
        setWarning({ record, rows });
      }
    } catch {
      setWarning({ record, rows: undefined });
    } finally {
      setChecking(false);
    }
  };

  const confirm = () => {
    if (warning !== undefined) {
      mutation.mutate({ id: warning.record.id, isActive: false });
    }
    setWarning(undefined);
  };

  return {
    request,
    confirm,
    dismiss: () => {
      setWarning(undefined);
    },
    warning,
    isBusy: mutation.isPending || isChecking,
  };
}
