import { useTranslation } from '@iplik/i18n';
import { Alert, Button, SectionCard, SimpleGrid, Stack, Text, TextInput } from '@iplik/ui';
import { useState } from 'react';
import { useRecordYarnReturn } from './hooks';
import type { DraftError } from './settlementDraft';
import { toReturnRequest } from './settlementDraft';
import { settlementErrorKey } from './settlementErrors';
import { useReturnDraft } from './useReturnDraft';

const NUMBER_STYLES = { input: { textAlign: 'right' } } as const;

export function YarnReturnForm({ shipmentId }: { shipmentId: string }) {
  const { t } = useTranslation();
  const { draft, field, reset } = useReturnDraft();
  const [error, setError] = useState<DraftError>();
  const mutation = useRecordYarnReturn(shipmentId);
  const submit = () => {
    const request = toReturnRequest(draft);
    setError(typeof request === 'string' ? request : undefined);
    if (typeof request !== 'string') mutation.mutate(request, { onSuccess: reset });
  };
  return (
    <SectionCard title={t('settlement.yarn.title')}>
      <Stack>
        <SimpleGrid cols={{ base: 1, sm: 2 }}>
          <TextInput label={t('settlement.yarn.yarnType')} withAsterisk {...field('yarnType')} />
          <TextInput label={t('settlement.yarn.lotId')} {...field('lotId')} />
          <TextInput
            label={t('settlement.yarn.netKg')}
            inputMode="decimal"
            styles={NUMBER_STYLES}
            withAsterisk
            {...field('netKg')}
          />
          <TextInput label={t('settlement.yarn.waybillNo')} {...field('waybillNo')} />
        </SimpleGrid>
        <Text size="sm" c="dimmed">
          {t('settlement.yarn.returnNote')}
        </Text>
        {error !== undefined && <Alert color="red">{t(`settlement.draftErrors.${error}`)}</Alert>}
        {mutation.isError && <Alert color="red">{t(settlementErrorKey(mutation.error))}</Alert>}
        <Button w="fit-content" loading={mutation.isPending} onClick={submit}>
          {t('settlement.yarn.submit')}
        </Button>
      </Stack>
    </SectionCard>
  );
}
