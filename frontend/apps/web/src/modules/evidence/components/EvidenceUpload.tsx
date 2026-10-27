import type { EvidenceSource, EvidenceType } from '@iplik/api-client';
import { EvidenceType as EVIDENCE_TYPES } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Paper, Select, Stack, Title } from '@iplik/ui';
import { useState } from 'react';
import { useCurrentUser } from '@/modules/auth';
import { useUploadEvidence } from '../hooks/useUploadEvidence';
import { MAX_SIZE_BYTES, UPLOAD_PERMISSION_BY_TYPE } from '../uploadTypes';
import { uploadErrorKey } from '../uploadErrors';
import { UploadButtons } from './UploadButtons';

/** Kullanıcının izni olan kanıt türleriyle fotoğraf/belge yükler; izni yoksa hiç görünmez. */
export function EvidenceUpload({ shipmentId }: { shipmentId: string }) {
  const { t } = useTranslation();
  const user = useCurrentUser();
  const allowed = Object.values(EVIDENCE_TYPES).filter((type) =>
    user.data?.permissions.includes(UPLOAD_PERMISSION_BY_TYPE[type]),
  );
  const [selected, setSelected] = useState<EvidenceType>();
  const [isTooLarge, setTooLarge] = useState(false);
  const mutation = useUploadEvidence(shipmentId);
  const evidenceType = selected ?? allowed[0];
  if (evidenceType === undefined) return null;
  const upload = (file: File | null, source: EvidenceSource) => {
    if (file === null) return;
    setTooLarge(file.size > MAX_SIZE_BYTES);
    if (file.size <= MAX_SIZE_BYTES) mutation.mutate({ file, evidenceType, source });
  };
  return (
    <Paper withBorder p="md">
      <Stack>
        <Title order={4}>{t('evidence.title')}</Title>
        <Select
          label={t('evidence.type')}
          data={allowed.map((value) => ({ value, label: t(`evidence.types.${value}`) }))}
          value={evidenceType}
          onChange={(value) => {
            setSelected(allowed.find((type) => type === value));
          }}
          allowDeselect={false}
        />
        <UploadButtons isLoading={mutation.isPending} onPick={upload} />
        {isTooLarge && <Alert color="red">{t('evidence.errors.tooLarge')}</Alert>}
        {mutation.isError && <Alert color="red">{t(uploadErrorKey(mutation.error))}</Alert>}
        {mutation.isSuccess && <Alert color="green">{t('evidence.done')}</Alert>}
      </Stack>
    </Paper>
  );
}
