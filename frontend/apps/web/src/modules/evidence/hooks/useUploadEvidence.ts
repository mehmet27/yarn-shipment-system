import type { EvidenceType, EvidenceView } from '@iplik/api-client';
import { confirmEvidenceUpload, requestEvidenceUpload, uploadToStorage } from '@iplik/api-client';
import { useMutation } from '@tanstack/react-query';

export type UploadRequest = { file: File; evidenceType: EvidenceType };

/** Üç adım: izin iste, dosyayı yükle, onayla. Sunucu hash'i kendisi hesaplar. */
export function useUploadEvidence(shipmentId: string) {
  return useMutation<EvidenceView, Error, UploadRequest>({
    mutationFn: async ({ file, evidenceType }) => {
      const ticket = await requestEvidenceUpload({
        shipmentId,
        evidenceType,
        mimeType: file.type,
        sizeBytes: file.size,
        capturedAt: new Date(file.lastModified).toISOString(),
      });
      await uploadToStorage(ticket.uploadUrl, ticket.headers, file);
      return confirmEvidenceUpload(ticket.evidenceId);
    },
  });
}
