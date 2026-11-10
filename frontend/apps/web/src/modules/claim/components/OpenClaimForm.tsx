import { useTranslation } from '@iplik/i18n';
import { parseKg } from '@iplik/units';
import { Alert, Button, Paper, Stack, Text, Textarea, Title } from '@iplik/ui';
import { TypedEvidenceUpload } from '@/modules/evidence';
import { useState } from 'react';
import { ClaimedKgField } from './ClaimedKgField';
import { claimErrorKey } from '../claimErrors';
import { useOpenClaim } from '../hooks/useClaims';

const MAX_DESCRIPTION_LENGTH = 2000;
const isZero = (kg: string) => Number(kg) === 0;

export type OpenClaimFormProps = {
  shipmentId: string;
  onDone: () => void;
};

/** Kalite itirazı (kg ile): itiraz edilen kilo, açıklama ve en az bir fotoğraf zorunlu (analiz Bölüm 10). */
export function OpenClaimForm({ shipmentId, onDone }: OpenClaimFormProps) {
  const { t } = useTranslation();
  const [kgText, setKgText] = useState('');
  const [description, setDescription] = useState('');
  const [evidenceIds, setEvidenceIds] = useState<string[]>([]);
  const mutation = useOpenClaim(shipmentId);
  const claimedKg = parseKg(kgText);
  const isKgInvalid = kgText.trim() !== '' && (claimedKg === undefined || isZero(claimedKg));
  const canSubmit = claimedKg !== undefined && !isZero(claimedKg) && description.trim() !== '' && evidenceIds.length > 0;
  return (
    <Paper withBorder p="md" component="section" aria-label={t('claim.open.title')}>
      <Stack>
        <Title order={3} size="h4">
          {t('claim.open.title')}
        </Title>
        <ClaimedKgField value={kgText} onChange={setKgText} isInvalid={isKgInvalid} />
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
    </Paper>
  );
}
