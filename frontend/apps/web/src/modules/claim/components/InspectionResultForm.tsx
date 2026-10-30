import type { InspectionView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Stack, Textarea } from '@iplik/ui';
import { TypedEvidenceUpload } from '@/modules/evidence';
import { useState } from 'react';
import { claimErrorKey } from '../claimErrors';
import { useRecordInspectionResult } from '../hooks/useClaims';

const MAX_RESULT_LENGTH = 2000;

export type InspectionResultFormProps = {
  claimId: string;
  shipmentId: string;
  inspection: InspectionView;
};

/** Sonuç bir kez yazılır; laboratuvar analizinde rapor belgesi (LAB_RESULT) zorunludur. */
export function InspectionResultForm({
  claimId,
  shipmentId,
  inspection,
}: InspectionResultFormProps) {
  const { t } = useTranslation();
  const [result, setResult] = useState('');
  const [evidenceId, setEvidenceId] = useState<string>();
  const mutation = useRecordInspectionResult(claimId, inspection.id);
  const isLab = inspection.kind === 'LAB';
  const canSubmit = result.trim() !== '' && (!isLab || evidenceId !== undefined);
  return (
    <Stack gap="xs">
      <Textarea
        label={t('claim.inspection.result')}
        maxLength={MAX_RESULT_LENGTH}
        minRows={2}
        value={result}
        onChange={(event) => {
          setResult(event.currentTarget.value);
        }}
        withAsterisk
      />
      <TypedEvidenceUpload
        shipmentId={shipmentId}
        evidenceType="LAB_RESULT"
        label={t(isLab ? 'claim.inspection.labDocument' : 'claim.inspection.optionalDocument')}
        doneLabel={t('evidence.done')}
        onUploaded={setEvidenceId}
      />
      {mutation.isError && <Alert color="red">{t(claimErrorKey(mutation.error))}</Alert>}
      <Button
        disabled={!canSubmit}
        loading={mutation.isPending}
        onClick={() => {
          const document = evidenceId === undefined ? {} : { evidenceId };
          mutation.mutate({ result: result.trim(), ...document });
        }}
      >
        {t('claim.inspection.saveResult')}
      </Button>
    </Stack>
  );
}
