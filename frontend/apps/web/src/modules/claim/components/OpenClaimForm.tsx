import { useTranslation } from '@iplik/i18n';
import { Alert, Button, SectionCard, SimpleGrid, Stack, Text, Textarea } from '@iplik/ui';
import { TypedEvidenceUpload } from '@/modules/evidence';
import { useState } from 'react';
import { ClaimedKgField } from './ClaimedKgField';
import { claimErrorKey } from '../claimErrors';
import { claimKgState } from '../claimLimit';
import { useOpenClaim } from '../hooks/useClaims';

const MAX_DESCRIPTION_LENGTH = 2000;

export type OpenClaimFormProps = {
  shipmentId: string;
  /** Sevkiyatın çıkış net kilosu; itiraz edilen kilo bunu aşamaz. */
  claimableKg: string;
  onDone: () => void;
};

/** Kalite itirazı (kg ile): itiraz edilen kilo, açıklama ve en az bir fotoğraf zorunlu (analiz Bölüm 10). */
export function OpenClaimForm({ shipmentId, claimableKg, onDone }: OpenClaimFormProps) {
  const { t } = useTranslation();
  const [kgText, setKgText] = useState('');
  const [description, setDescription] = useState('');
  const [evidenceIds, setEvidenceIds] = useState<string[]>([]);
  const mutation = useOpenClaim(shipmentId);
  const kg = claimKgState(kgText, claimableKg);
  const { claimedKg } = kg;
  const canSubmit =
    kg.isUsable && !kg.isOverLimit && description.trim() !== '' && evidenceIds.length > 0;
  return (
    <SectionCard title={t('claim.open.title')}>
      <Stack>
        <SimpleGrid cols={{ base: 1, sm: 2 }}>
          <ClaimedKgField
            value={kgText}
            onChange={setKgText}
            isInvalid={kg.isInvalid}
            limit={kg.limit}
            isOverLimit={kg.isOverLimit}
          />
        </SimpleGrid>
        <Textarea
          label={t('claim.open.description')}
          maxLength={MAX_DESCRIPTION_LENGTH}
          minRows={3}
          value={description}
          onChange={(event) => {
            setDescription(event.currentTarget.value);
          }}
          withAsterisk
        />
        <TypedEvidenceUpload
          shipmentId={shipmentId}
          evidenceType="QUALITY_CLAIM"
          label={t('claim.open.photos')}
          doneLabel={t('claim.open.photosDone', { count: evidenceIds.length })}
          onUploaded={(id) => {
            setEvidenceIds((current) => [...current, id]);
          }}
        />
        {evidenceIds.length === 0 && <Text c="dimmed">{t('claim.open.photoRequired')}</Text>}
        {mutation.isError && <Alert color="red">{t(claimErrorKey(mutation.error))}</Alert>}
        <Button
          disabled={!canSubmit}
          loading={mutation.isPending}
          onClick={() => {
            if (claimedKg === undefined) return;
            const input = { description: description.trim(), claimedKg, evidenceIds };
            mutation.mutate(input, { onSuccess: onDone });
          }}
        >
          {t('claim.open.submit')}
        </Button>
      </Stack>
    </SectionCard>
  );
}
