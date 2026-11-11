import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Paper, Select, Stack, Text, TextInput, Title } from '@iplik/ui';
import { useState } from 'react';
import { useRecordYarnReturn } from './hooks';
import type { DraftError } from './settlementDraft';
import { toReturnRequest } from './settlementDraft';
import { settlementErrorKey } from './settlementErrors';
import { useReturnDraft } from './useReturnDraft';

const DIRECTIONS = ['RETURN', 'FROM_SUBCONTRACTOR'] as const;

export function YarnReturnForm({ shipmentId }: { shipmentId: string }) {
  const { t } = useTranslation();
  const { draft, field, setDirection, reset } = useReturnDraft();
  const [error, setError] = useState<DraftError>();
  const mutation = useRecordYarnReturn(shipmentId);
  const submit = () => {
    const request = toReturnRequest(draft);
    setError(typeof request === 'string' ? request : undefined);
    if (typeof request !== 'string') mutation.mutate(request, { onSuccess: reset });
  };
  return (
    <Paper withBorder p="md" component="section" aria-label={t('settlement.yarn.title')}>
      <Stack>
        <Title order={3} size="h4">
          {t('settlement.yarn.title')}
        </Title>
        <Select
          label={t('settlement.yarn.direction')}
          data={DIRECTIONS.map((value) => ({
            value,
            label: t(`settlement.yarn.directions.${value}`),
          }))}
          value={draft.direction}
          allowDeselect={false}
          onChange={(value) => {
            const direction = DIRECTIONS.find((item) => item === value);
            if (direction !== undefined) setDirection(direction);
          }}
        />
        <TextInput label={t('settlement.yarn.yarnType')} withAsterisk {...field('yarnType')} />
        <TextInput label={t('settlement.yarn.lotId')} {...field('lotId')} />
        <TextInput
          label={t('settlement.yarn.netKg')}
          inputMode="decimal"
          withAsterisk
          {...field('netKg')}
        />
        <TextInput label={t('settlement.yarn.waybillNo')} {...field('waybillNo')} />
        <Text size="sm" c="dimmed">
          {t('settlement.yarn.returnNote')}
        </Text>
        {error !== undefined && <Alert color="red">{t(`settlement.draftErrors.${error}`)}</Alert>}
        {mutation.isError && <Alert color="red">{t(settlementErrorKey(mutation.error))}</Alert>}
        <Button w="fit-content" loading={mutation.isPending} onClick={submit}>
          {t('settlement.yarn.submit')}
        </Button>
      </Stack>
    </Paper>
  );
}
