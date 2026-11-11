import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Paper, Stack, TextInput, Title } from '@iplik/ui';
import { useState } from 'react';
import { FabricYarnRows } from './FabricYarnRows';
import { useRecordFabricReceipt } from './hooks';
import type { DraftError } from './settlementDraft';
import { toFabricRequest } from './settlementDraft';
import { settlementErrorKey } from './settlementErrors';
import { useFabricDraft } from './useFabricDraft';

/** Kumaş girişi formu; iplik dağılımı düzenlenebilir, toplamı kumaş kilosuna eşit olmalı. */
export function FabricForm({ shipmentId }: { shipmentId: string }) {
  const { t } = useTranslation();
  const { draft, field, setYarn, addYarn, removeYarn, reset } = useFabricDraft();
  const [error, setError] = useState<DraftError>();
  const mutation = useRecordFabricReceipt(shipmentId);
  const submit = () => {
    const request = toFabricRequest(draft);
    setError(typeof request === 'string' ? request : undefined);
    if (typeof request !== 'string') mutation.mutate(request, { onSuccess: reset });
  };
  return (
    <Paper withBorder p="md" component="section" aria-label={t('settlement.fabric.title')}>
      <Stack>
        <Title order={3} size="h4">
          {t('settlement.fabric.title')}
        </Title>
        <TextInput
          label={t('settlement.fabric.fabricType')}
          withAsterisk
          {...field('fabricType')}
        />
        <Group grow align="flex-start">
          <TextInput
            label={t('settlement.fabric.netKg')}
            inputMode="decimal"
            withAsterisk
            {...field('netKg')}
          />
          <TextInput
            label={t('settlement.fabric.rollCount')}
            inputMode="numeric"
            {...field('rollCount')}
          />
        </Group>
        <TextInput label={t('settlement.fabric.waybillNo')} withAsterisk {...field('waybillNo')} />
        <FabricYarnRows
          rows={draft.yarns}
          onChange={setYarn}
          onAdd={addYarn}
          onRemove={removeYarn}
        />
        {error !== undefined && <Alert color="red">{t(`settlement.draftErrors.${error}`)}</Alert>}
        {mutation.isError && <Alert color="red">{t(settlementErrorKey(mutation.error))}</Alert>}
        <Button w="fit-content" loading={mutation.isPending} onClick={submit}>
          {t('settlement.fabric.submit')}
        </Button>
      </Stack>
    </Paper>
  );
}
