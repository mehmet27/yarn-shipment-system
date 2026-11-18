import { useTranslation } from '@iplik/i18n';
import { Alert, SectionCard, Stack, Text } from '@iplik/ui';
import { useState } from 'react';
import { useCurrentUser } from '@/modules/auth';
import { useUploadEvidence } from '../hooks/useUploadEvidence';
import { ACCEPTED_MIME_TYPES, fileProblem, UPLOAD_PERMISSION_BY_TYPE } from '../uploadTypes';
import type { FileProblem } from '../uploadTypes';
import { uploadErrorKey } from '../uploadErrors';
import { UploadButtons } from './UploadButtons';

export type EvidenceUploadProps = {
  shipmentId: string;
  /** Çıkış tartısı girildi mi; kantar fişi fotoğrafı ilk kez tartıyla birlikte yüklendiği için önce sunulmaz. */
  isDispatched: boolean;
};

/**
 * Kantar fişini yenileme: yeni fiş yüklenince öncekisi sunucuda geçersiz sayılır. Kalite itirazı fotoğrafı ve
 * laboratuvar belgesi bu bölümden değil, "Kalite itirazı" sekmesinden girilir; irsaliye tamamlama formundan gelir.
 * Yükleme izni yoksa hiç görünmez.
 */
export function EvidenceUpload({ shipmentId, isDispatched }: EvidenceUploadProps) {
  const { t } = useTranslation();
  const user = useCurrentUser();
  const [problem, setProblem] = useState<FileProblem>();
  const mutation = useUploadEvidence(shipmentId);
  const canUpload = user.data?.permissions.includes(UPLOAD_PERMISSION_BY_TYPE.DISPATCH_SCALE);
  if (!isDispatched || canUpload !== true) return null;
  const upload = (file: File | null) => {
    if (file === null) return;
    const found = fileProblem(file, ACCEPTED_MIME_TYPES);
    setProblem(found);
    if (found === undefined) mutation.mutate({ file, evidenceType: 'DISPATCH_SCALE' });
  };
  return (
    <SectionCard title={t('evidence.scale.title')}>
      <Stack>
        <Text size="sm" c="dimmed">
          {t('evidence.scale.hint')}
        </Text>
        <UploadButtons isLoading={mutation.isPending} isDisabled={false} onPick={upload} />
        {problem !== undefined && <Alert color="red">{t(`evidence.errors.${problem}`)}</Alert>}
        {mutation.isError && <Alert color="red">{t(uploadErrorKey(mutation.error))}</Alert>}
        {mutation.isSuccess && <Alert color="green">{t('evidence.done')}</Alert>}
      </Stack>
    </SectionCard>
  );
}
