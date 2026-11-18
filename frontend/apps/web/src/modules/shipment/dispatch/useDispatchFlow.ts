import { toProblem } from '@iplik/api-client';
import { useState } from 'react';
import { useRecordDispatch } from '../hooks/useRecordDispatch';
import type { DispatchDraft, DispatchError } from './dispatchDraft';
import { EMPTY_DISPATCH_DRAFT, validateDispatch } from './dispatchDraft';
import { sendDispatch } from './dispatchSubmit';
import { useBagMismatch } from './useBagMismatch';
import { useDispatchWeighing } from './useDispatchWeighing';

/**
 * Çıkış tartısı akışı: "İleri" alanları doğrular ve (eksik kilo nedeni dahil) onay adımını açar; onaylanınca
 * kaydedilir. Beklenen net ve tolerans sunucudakiyle aynı kuralla karşılaştırılır, asıl karar sunucudadır.
 */
export function useDispatchFlow(
  shipmentId: string,
  plannedNetKg: string | undefined,
  onSaved: () => void,
) {
  const [draft, setDraft] = useState<DispatchDraft>(EMPTY_DISPATCH_DRAFT);
  const [error, setError] = useState<DispatchError>();
  const [isPhotoMissing, setPhotoMissing] = useState(false);
  const [isConfirming, setConfirming] = useState(false);
  const [isReasonEmpty, setReasonEmpty] = useState(false);
  const mutation = useRecordDispatch(shipmentId);
  const bagMismatch = useBagMismatch(shipmentId);
  const { weighing, comparison } = useDispatchWeighing(draft, plannedNetKg);
  const review = () => {
    const invalid = validateDispatch(draft);
    setError(invalid);
    const isBlocked = invalid === undefined && bagMismatch.check(weighing?.netKg);
    const isReasonRequired = comparison?.isShort === true && draft.shortfallReason.trim() === '';
    setReasonEmpty(invalid === undefined && !isBlocked && isReasonRequired);
    setConfirming(invalid === undefined && !isBlocked && !isReasonRequired);
  };
  const confirm = () => {
    sendDispatch(draft, mutation, {
      done: (isUploaded) => {
        setConfirming(false);
        if (isUploaded) onSaved();
        else setPhotoMissing(true);
      },
      rejected: (problem) => {
        if (bagMismatch.reject(problem)) setConfirming(false);
      },
    });
  };
  return {
    draft,
    change: (next: DispatchDraft) => {
      setDraft(next);
      setReasonEmpty(false);
    },
    error,
    weighing,
    comparison,
    mutation,
    bagMismatch,
    isPhotoMissing,
    isConfirming,
    closeConfirm: () => {
      setConfirming(false);
    },
    isReasonMissing:
      isReasonEmpty ||
      (mutation.isError && toProblem(mutation.error).code === 'SHORTFALL_REASON_REQUIRED'),
    review,
    confirm,
  };
}
