import { useState } from 'react';
import { useConfirmStep } from '@/modules/shared';
import { useRecordYarnReturn } from './hooks';
import type { DraftError, YarnReturnDraft } from './settlementDraft';
import { toReturnRequest } from './settlementDraft';

/** İplik dönüşü: önce denetim ve onay sorusu, sonra kayıt. */
export function useReturnSubmit(shipmentId: string, draft: YarnReturnDraft, reset: () => void) {
  const [error, setError] = useState<DraftError>();
  const mutation = useRecordYarnReturn(shipmentId);
  const request = toReturnRequest(draft);
  const step = useConfirmStep(request);
  const submit = () => {
    setError(typeof request === 'string' ? request : undefined);
    if (typeof request !== 'string') step.ask();
  };
  const confirm = () => {
    if (typeof request === 'string') return;
    mutation.mutate(request, { onSuccess: reset, onSettled: step.close });
  };
  return { error, mutation, step, submit, confirm };
}
