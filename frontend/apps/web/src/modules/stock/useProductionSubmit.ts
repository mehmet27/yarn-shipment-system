import { useRef, useState } from 'react';
import { useConfirmStep } from '@/modules/shared';
import { useFireRateDefault } from '@/modules/shipment';
import { useRecordCentralProduction } from './hooks/useCentralStock';
import type { StockDraftError } from './stockDraft';
import { toProductionRequest } from './stockDraft';
import { useProductionDraft } from './useProductionDraft';

/** Merkez kumaş girişi taslağı ve gönderimi; çift tıklama aynı üretimi iki kez yazmaz. */
export function useProductionSubmit() {
  const defaultFirePct = useFireRateDefault();
  const draftState = useProductionDraft(defaultFirePct);
  const [error, setError] = useState<StockDraftError>();
  const mutation = useRecordCentralProduction();
  // İkinci tıklama, düğme yüklenme durumuna geçmeden gelir; bu yüzden durum bir ref'te tutulur.
  const busy = useRef(false);
  const step = useConfirmStep(toProductionRequest(draftState.draft));
  /** İlk adım yalnız denetler ve onay sorar. */
  const submit = () => {
    // reset() uçuştaki isteğin geri çağrılarını düşürür; meşgulken hiçbir şey yapılmaz.
    if (busy.current) return;
    mutation.reset();
    const request = toProductionRequest(draftState.draft);
    setError(typeof request === 'string' ? request : undefined);
    if (typeof request !== 'string') step.ask();
  };
  const confirm = () => {
    const request = toProductionRequest(draftState.draft);
    if (busy.current || typeof request === 'string') return;
    busy.current = true;
    mutation.mutate(request, {
      onSuccess: draftState.reset,
      onSettled: () => {
        busy.current = false;
        step.close();
      },
    });
  };
  return { ...draftState, error, submit, confirm, step, mutation };
}
