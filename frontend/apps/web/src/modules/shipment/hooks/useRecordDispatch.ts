import type { DispatchWeighingRequest } from '@iplik/api-client';
import { recordDispatchWeighing, SHIPMENTS_PATH } from '@iplik/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useUploadEvidence } from '@/modules/evidence';

export type DispatchSubmission = { request: DispatchWeighingRequest; photo: File };

/** `photoUploaded` yanlışsa tartı kaydedildi ama kantar fişi fotoğrafı yüklenemedi. */
export type DispatchResult = { photoUploaded: boolean };

/**
 * Çıkış tartısını kaydeder. Fotoğraf yalnızca sunucu tartıyı kabul ettikten sonra (tüm kurallar geçince) yüklenir;
 * reddedilen tartıda sisteme hiçbir şey gitmez. Başarıdan sonra sevkiyat yeniden okunur.
 */
export function useRecordDispatch(shipmentId: string) {
  const queryClient = useQueryClient();
  const upload = useUploadEvidence(shipmentId);
  return useMutation({
    mutationFn: async ({ request, photo }: DispatchSubmission): Promise<DispatchResult> => {
      await recordDispatchWeighing(shipmentId, request);
      try {
        await upload.mutateAsync({ file: photo, evidenceType: 'DISPATCH_SCALE' });
        return { photoUploaded: true };
      } catch {
        return { photoUploaded: false };
      }
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [SHIPMENTS_PATH] }),
  });
}
