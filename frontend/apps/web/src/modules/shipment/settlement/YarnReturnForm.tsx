import { useTranslation } from '@iplik/i18n';
import { Alert, Button, SectionCard, Select, SimpleGrid, Stack, Text, TextInput } from '@iplik/ui';
import { useState } from 'react';
import { useRecordYarnReturn } from './hooks';
import type { DraftError } from './settlementDraft';
import { toReturnRequest } from './settlementDraft';
import { settlementErrorKey } from './settlementErrors';
import { useReturnDraft } from './useReturnDraft';
import { useReturnOptions } from './useReturnOptions';

const NUMBER_STYLES = { input: { textAlign: 'right' } } as const;

export function YarnReturnForm({ shipmentId }: { shipmentId: string }) {
  const { t } = useTranslation();
  const { draft, field, setYarnType, setLotId, reset } = useReturnDraft();
  const [error, setError] = useState<DraftError>();
  const mutation = useRecordYarnReturn(shipmentId);
  const { yarns, lots, yarnType, lotId } = useReturnOptions(shipmentId, draft);
  const submit = () => {
    const request = toReturnRequest({ ...draft, yarnType, lotId });
    setError(typeof request === 'string' ? request : undefined);
    if (typeof request !== 'string') mutation.mutate(request, { onSuccess: reset });
  };
  return (
    <SectionCard title={t('settlement.yarn.title')}>
      <Stack>
        <SimpleGrid cols={{ base: 1, sm: 2 }}>
          <Select
            label={t('settlement.yarn.yarnType')}
            placeholder={t('settlement.fabric.yarnPlaceholder')}
            data={yarns}
            value={yarnType === '' ? null : yarnType}
            onChange={(value) => {
              setYarnType(value ?? '');
            }}
            allowDeselect={false}
            nothingFoundMessage={t('settlement.fabric.noYarnFound')}
            withAsterisk
          />
          {lots.length > 0 && (
            <Select
              label={t('settlement.yarn.lotId')}
              data={lots}
              value={lotId === '' ? null : lotId}
              onChange={(value) => {
                setLotId(value ?? '');
              }}
              clearable
            />
          )}
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
