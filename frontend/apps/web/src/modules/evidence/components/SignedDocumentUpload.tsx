import { useTranslation } from '@iplik/i18n';
import { Alert, Stack, Text } from '@iplik/ui';
import { useState } from 'react';
import { useUploadEvidence } from '../hooks/useUploadEvidence';
import { MAX_SIZE_BYTES } from '../uploadTypes';
import { uploadErrorKey } from '../uploadErrors';
import { UploadButtons } from './UploadButtons';

export type SignedDocumentUploadProps = {
  shipmentId: string;
  /** Yükleme onaylanınca kanıt kimliği; teslim isteğine bağlanır. */
  onUploaded: (evidenceId: string) => void;
};

/** İmzalı irsaliye fotoğrafı/PDF'i: yükleyip onaylar, dönen kanıt kimliğini üst bileşene verir. */
export function SignedDocumentUpload({ shipmentId, onUploaded }: SignedDocumentUploadProps) {
  const { t } = useTranslation();
  const mutation = useUploadEvidence(shipmentId);
  const [isTooLarge, setTooLarge] = useState(false);
  const pick = (file: File | null, source: 'CAMERA' | 'GALLERY') => {
    if (file === null) return;
    setTooLarge(file.size > MAX_SIZE_BYTES);
    if (file.size > MAX_SIZE_BYTES) return;
    mutation.mutate(
      { file, evidenceType: 'SIGNED_DELIVERY_DOC', source },
      {
        onSuccess: (evidence) => {
          onUploaded(evidence.id);
        },
      },
    );
  };
  return (
    <Stack>
      <Text fw={600}>{t('delivery.document.signed')}</Text>
      <UploadButtons isLoading={mutation.isPending} onPick={pick} />
      {isTooLarge && <Alert color="red">{t('evidence.errors.tooLarge')}</Alert>}
      {mutation.isError && <Alert color="red">{t(uploadErrorKey(mutation.error))}</Alert>}
      {mutation.isSuccess && <Alert color="green">{t('delivery.document.signedDone')}</Alert>}
    </Stack>
  );
}
