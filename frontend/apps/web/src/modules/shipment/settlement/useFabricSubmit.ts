import { useState } from 'react';
import { exceedsRemaining } from './fabricLimits';
import { useRecordFabricReceipt } from './hooks';
import type { DraftError } from './settlementDraft';
import { toFabricRequest } from './settlementDraft';
import { useFabricDraft } from './useFabricDraft';
import { useFabricLimits } from './useFabricLimits';

/** Kumaş girişi: taslak, sınırlar ve iki adımlı kayıt (önce kontrol + onay sorusu, sonra gönderim). */
export function useFabricSubmit(shipmentId: string) {
  const draftState = useFabricDraft();
  const { draft, reset } = draftState;
  const limits = useFabricLimits(shipmentId);
  const [error, setError] = useState<DraftError>();
  const [isConfirming, setConfirming] = useState(false);
  const mutation = useRecordFabricReceipt(shipmentId);
  const request = toFabricRequest(draft, limits.remainingKg);
  /** Sunucu kuralına göre iplik taşmasını alan altında hemen gösterir. */
  const isOverLimit = exceedsRemaining(draft.netKg, limits.remainingKg);
  const ask = () => {
    mutation.reset();
    setError(typeof request === 'string' ? request : undefined);
    setConfirming(typeof request !== 'string');
  };
  const confirm = () => {
    if (typeof request === 'string') return;
    mutation.mutate(request, {
      onSuccess: reset,
      onSettled: () => {
        setConfirming(false);
      },
    });
  };
  const cancel = () => {
    setConfirming(false);
  };
  return {
    ...draftState,
    limits,
    error,
    isConfirming,
    isOverLimit,
    request,
    mutation,
    ask,
    confirm,
    cancel,
  };
}
