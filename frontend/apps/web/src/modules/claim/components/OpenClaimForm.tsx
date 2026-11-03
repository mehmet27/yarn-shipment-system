import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Checkbox, Group, Paper, Stack, Text, Textarea, Title } from '@iplik/ui';
import { TypedEvidenceUpload } from '@/modules/evidence';
import { useState } from 'react';
import { claimErrorKey } from '../claimErrors';
import { useOpenClaim } from '../hooks/useClaims';

const MAX_DESCRIPTION_LENGTH = 2000;

export type OpenClaimFormProps = {
  shipmentId: string;
  /** Teslimde okutulmuş ve henüz itiraz edilmemiş çuval anahtarları. */
  bagKeys: readonly string[];
  onDone: () => void;
};

/** Kalite itirazı: çuval seçimi, açıklama ve en az bir fotoğraf zorunlu (analiz Bölüm 10). */
export function OpenClaimForm({ shipmentId, bagKeys, onDone }: OpenClaimFormProps) {
  const { t } = useTranslation();
  const [selected, setSelected] = useState<string[]>([]);
  const [description, setDescription] = useState('');
  const [evidenceIds, setEvidenceIds] = useState<string[]>([]);
  const mutation = useOpenClaim(shipmentId);
  const canSubmit = selected.length > 0 && description.trim() !== '' && evidenceIds.length > 0;
  return (
    <Paper withBorder p="md" component="section" aria-label={t('claim.open.title')}>
      <Stack>
        <Title order={3} size="h4">
          {t('claim.open.title')}
        </Title>
        <Checkbox.Group
          label={t('claim.open.bags')}
          value={selected}
          onChange={setSelected}
          withAsterisk
        >
          <Group mt="xs">
            {bagKeys.map((key) => (
              <Checkbox key={key} value={key} label={key} />
            ))}
          </Group>
        </Checkbox.Group>
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
            const input = { description: description.trim(), bagKeys: selected, evidenceIds };
            mutation.mutate(input, { onSuccess: onDone });
          }}
        >
          {t('claim.open.submit')}
        </Button>
      </Stack>
    </Paper>
  );
}
