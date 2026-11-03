import type { EvidenceType } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Stack, Text } from '@iplik/ui';
import { useState } from 'react';
import { useUploadEvidence } from '../hooks/useUploadEvidence';
import { ACCEPTED_MIME_TYPES, fileProblem } from '../uploadTypes';
import type { FileProblem } from '../uploadTypes';
import { uploadErrorKey } from '../uploadErrors';
import { UploadButtons } from './UploadButtons';

export type TypedEvidenceUploadProps = {
  shipmentId: string;
  /** Türü ekran belirler; kullanıcıya tür sorulmaz. */
  evidenceType: EvidenceType;
  label: string;
  doneLabel: string;
  /** Yükleme onaylanınca kanıt kimliği; üst bileşen isteğe bağlar. */
  onUploaded: (evidenceId: string) => void;
};

/** Türü sabit kanıt yükleme: yükleyip onaylar, dönen kanıt kimliğini üst bileşene verir. */
export function TypedEvidenceUpload({
  shipmentId,
  evidenceType,
  label,
  doneLabel,
  onUploaded,
}: TypedEvidenceUploadProps) {
  const { t } = useTranslation();
  const mutation = useUploadEvidence(shipmentId);
  const [problem, setProblem] = useState<FileProblem>();
  const pick = (file: File | null, source: 'CAMERA' | 'GALLERY') => {
    if (file === null) return;
    const found = fileProblem(file, ACCEPTED_MIME_TYPES);
    setProblem(found);
    if (found !== undefined) return;
    mutation.mutate(
      { file, evidenceType, source },
      {
        onSuccess: (evidence) => {
          onUploaded(evidence.id);
        },
      },
    );
  };
  return (
    <Stack>
      <Text fw={600}>{label}</Text>
      <UploadButtons isLoading={mutation.isPending} onPick={pick} />
      {problem !== undefined && <Alert color="red">{t(`evidence.errors.${problem}`)}</Alert>}
      {mutation.isError && <Alert color="red">{t(uploadErrorKey(mutation.error))}</Alert>}
      {mutation.isSuccess && <Alert color="green">{doneLabel}</Alert>}
    </Stack>
  );
}
