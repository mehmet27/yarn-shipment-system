import type { EvidenceType } from '@iplik/api-client';
import { EvidenceType as EVIDENCE_TYPES } from '@iplik/api-client';
import { todayIsoDate, useTranslation } from '@iplik/i18n';
import { Alert, Paper, Select, Stack, Title } from '@iplik/ui';
import { useState } from 'react';
import { useCurrentUser } from '@/modules/auth';
import { useUploadEvidence } from '../hooks/useUploadEvidence';
import { ACCEPTED_MIME_TYPES, fileProblem, UPLOAD_PERMISSION_BY_TYPE } from '../uploadTypes';
import type { FileProblem } from '../uploadTypes';
import { uploadErrorKey } from '../uploadErrors';
import { NotDueAlert } from './NotDueAlert';
import { UploadButtons } from './UploadButtons';

/** İrsaliye yalnızca tamamlama formundan, kendi akışıyla yüklenir. */
const OWN_FLOW_TYPES: ReadonlySet<EvidenceType> = new Set(['WAYBILL']);

/** Kalite itirazı fotoğrafı ve laboratuvar sonucu yalnızca tamamlanmış sevkiyata eklenir (sunucu da böyle denetler). */
const CLAIM_TYPES: ReadonlySet<EvidenceType> = new Set(['QUALITY_CLAIM', 'LAB_RESULT']);

function soleType(allowed: readonly EvidenceType[]): EvidenceType | undefined {
  return allowed.length === 1 ? allowed[0] : undefined;
}

/** Depo çıkış fotoğrafı sevkiyat gününden önce yüklenemez. */
function isDispatchPhotoTooEarly(type: EvidenceType | undefined, shipmentDate: string): boolean {
  return type === 'DISPATCH_SCALE' && shipmentDate > todayIsoDate();
}

export type EvidenceUploadProps = {
  shipmentId: string;
  /** Sevkiyat tamamlandı mı; değilse itiraz türleri sunulmaz. */
  isCompleted: boolean;
  /** Sevkiyat günü (YYYY-AA-GG); gün gelmeden çıkış fotoğrafı yüklenemez (sunucu da böyle denetler). */
  shipmentDate: string;
};

/** Kullanıcının izni olan kanıt türleriyle fotoğraf/belge yükler; izni yoksa hiç görünmez. */
export function EvidenceUpload({ shipmentId, isCompleted, shipmentDate }: EvidenceUploadProps) {
  const { t } = useTranslation();
  const user = useCurrentUser();
  const allowed = Object.values(EVIDENCE_TYPES).filter(
    (type) =>
      !OWN_FLOW_TYPES.has(type) &&
      (isCompleted || !CLAIM_TYPES.has(type)) &&
      user.data?.permissions.includes(UPLOAD_PERMISSION_BY_TYPE[type]),
  );
  const [selected, setSelected] = useState<EvidenceType>();
  const [problem, setProblem] = useState<FileProblem>();
  const mutation = useUploadEvidence(shipmentId);
  // Tek seçenek varsa o seçilidir; birden çoksa yanlış türde yükleme olmasın diye kullanıcı kendisi seçer.
  const evidenceType = selected ?? soleType(allowed);
  if (allowed.length === 0) return null;
  const isNotDue = isDispatchPhotoTooEarly(evidenceType, shipmentDate);
  const upload = (file: File | null) => {
    if (file === null || evidenceType === undefined) return;
    const found = fileProblem(file, ACCEPTED_MIME_TYPES);
    setProblem(found);
    if (found === undefined) mutation.mutate({ file, evidenceType });
  };
  return (
    <Paper withBorder p="md">
      <Stack>
        <Title order={4}>{t('evidence.title')}</Title>
        <Select
          label={t('evidence.type')}
          data={allowed.map((value) => ({ value, label: t(`evidence.types.${value}`) }))}
          placeholder={t('evidence.typePlaceholder')}
          value={evidenceType ?? null}
          onChange={(value) => {
            setSelected(allowed.find((type) => type === value));
          }}
          allowDeselect={false}
        />
        <UploadButtons
          isLoading={mutation.isPending}
          isDisabled={evidenceType === undefined || isNotDue}
          onPick={upload}
        />
        {isNotDue && <NotDueAlert />}
        {problem !== undefined && <Alert color="red">{t(`evidence.errors.${problem}`)}</Alert>}
        {mutation.isError && <Alert color="red">{t(uploadErrorKey(mutation.error))}</Alert>}
        {mutation.isSuccess && <Alert color="green">{t('evidence.done')}</Alert>}
      </Stack>
    </Paper>
  );
}
