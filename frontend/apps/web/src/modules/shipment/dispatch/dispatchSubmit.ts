import { toProblem } from '@iplik/api-client';
import type { useRecordDispatch } from '../hooks/useRecordDispatch';
import type { DispatchDraft } from './dispatchDraft';
import { toDispatchRequest } from './dispatchDraft';

export function failureKey(error: unknown): string {
  const { code } = toProblem(error);
  if (code === 'INVALID_SHIPMENT_STATUS') return 'dispatch.notDraft';
  if (code === 'SHIPMENT_NOT_DUE') return 'dispatch.notDue';
  if (code === 'INVALID_DISPATCH') return 'dispatch.inconsistent';
  if (code === 'SHORTFALL_REASON_REQUIRED') return 'dispatch.shortfallReasonRequired';
  if (code === 'BAG_WEIGHT_MISMATCH') return 'dispatch.bagMismatch.alert';
  return 'dispatch.submitFailed';
}

type DispatchMutation = ReturnType<typeof useRecordDispatch>;

/** Geçerli taslağı gönderir; fotoğraf yüklendi mi bilgisini `done` ile bildirir. */
export function sendDispatch(
  draft: DispatchDraft,
  mutation: DispatchMutation,
  callbacks: {
    done: (isPhotoUploaded: boolean) => void;
    rejected: (error: unknown) => void;
  },
) {
  const request = toDispatchRequest(draft);
  if (request === undefined || draft.ticket === undefined) return;
  mutation.mutate(
    { request, photo: draft.ticket },
    {
      onSuccess: (result) => {
        callbacks.done(result.photoUploaded);
      },
      onError: callbacks.rejected,
    },
  );
}
