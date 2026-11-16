import type { DispatchWeighingRequest } from '@iplik/api-client';
import { recordDispatchWeighing, SHIPMENTS_PATH } from '@iplik/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useRef } from 'react';
import { useUploadEvidence } from '@/modules/evidence';

export type DispatchSubmission = { request: DispatchWeighingRequest; photo: File };

/**
 * Çıkış tartısını kaydeder. Kantar fişi fotoğrafı ancak kayıt onaylanınca yüklenir (önce fotoğraf, sonra tartı);
 * tartı reddedilirse aynı fotoğraf yeniden yüklenmez. Başarıdan sonra sevkiyat yeniden okunur.
 */
export function useRecordDispatch(shipmentId: string) {
  const queryClient = useQueryClient();
  const upload = useUploadEvidence(shipmentId);
  const uploaded = useRef<File | undefined>(undefined);
  return useMutation({
    mutationFn: async ({ request, photo }: DispatchSubmission) => {
      if (uploaded.current !== photo) {
        await upload.mutateAsync({ file: photo, evidenceType: 'DISPATCH_SCALE' });
        uploaded.current = photo;
      }
      return recordDispatchWeighing(shipmentId, request);
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [SHIPMENTS_PATH] }),
  });
}
