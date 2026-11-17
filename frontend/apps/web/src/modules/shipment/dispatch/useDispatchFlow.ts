import { toProblem } from '@iplik/api-client';
import { compareWeighing } from '@iplik/units';
import { useState } from 'react';
import { useRecordDispatch } from '../hooks/useRecordDispatch';
import { useWeightTolerance } from '../hooks/useWeightTolerance';
import type { DispatchDraft, DispatchError } from './dispatchDraft';
import { EMPTY_DISPATCH_DRAFT, parseDispatchWeighing, validateDispatch } from './dispatchDraft';
import { sendDispatch } from './dispatchSubmit';

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
  const tolerance = useWeightTolerance();
  const mutation = useRecordDispatch(shipmentId);
  const parsed = parseDispatchWeighing(draft);
  const weighing = typeof parsed === 'string' ? undefined : parsed;
  const comparison =
    weighing === undefined || plannedNetKg === undefined
      ? undefined
      : compareWeighing(weighing.netKg, plannedNetKg, tolerance);
  const review = () => {
    const invalid = validateDispatch(draft);
    setError(invalid);
    const isReasonRequired = comparison?.isShort === true && draft.shortfallReason.trim() === '';
    setReasonEmpty(invalid === undefined && isReasonRequired);
    setConfirming(invalid === undefined && !isReasonRequired);
  };
  const confirm = () => {
    sendDispatch(draft, mutation, (isUploaded) => {
      setConfirming(false);
      if (isUploaded) onSaved();
      else setPhotoMissing(true);
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
