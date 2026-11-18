import { useTranslation } from '@iplik/i18n';
import { Alert, SectionCard, Stack, Text } from '@iplik/ui';
import { useState } from 'react';
import { useCanRefreshTicket } from '../hooks/useCanRefreshTicket';
import { useUploadEvidence } from '../hooks/useUploadEvidence';
import { ACCEPTED_MIME_TYPES, fileProblem } from '../uploadTypes';
import type { FileProblem } from '../uploadTypes';
import { uploadErrorKey } from '../uploadErrors';
import { UploadButtons } from './UploadButtons';

export type EvidenceUploadProps = {
  shipmentId: string;
  /** Çıkış tartısı girildi mi; kantar fişi fotoğrafı ilk kez tartıyla birlikte yüklendiği için önce sunulmaz. */
  isDispatched: boolean;
  /** Sevkiyat yola çıktı mı (yolda, tamamlanmış ya da iptal); fiş bundan sonra yenilenmez. */
  isDeparted: boolean;
};

/**
 * Kantar fişini yenileme: yeni fiş yüklenince öncekisi sunucuda geçersiz sayılır. Fiş varsa yalnızca yola çıkmamış
 * sevkiyatta sunulur (sunucu ayrıca yalnız ilk fişi yükleyenin yenileyebildiğini denetler); fiş hiç yüklenememişse
 * yolda olsa da yeniden denenebilir. Kalite itirazı fotoğrafı ve
 * laboratuvar belgesi bu bölümden değil, "Kalite itirazı" sekmesinden girilir; irsaliye tamamlama formundan gelir.
 * Yükleme izni yoksa hiç görünmez.
 */
export function EvidenceUpload({ shipmentId, isDispatched, isDeparted }: EvidenceUploadProps) {
  const { t } = useTranslation();
  const [problem, setProblem] = useState<FileProblem>();
  const mutation = useUploadEvidence(shipmentId);
  const canRefresh = useCanRefreshTicket(shipmentId, isDispatched, isDeparted);
  if (!canRefresh) return null;
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
