import { MAX_WAYBILL_NO_LENGTH } from '../waybill';
import { ReturnActions } from './ReturnActions';
import { useTranslation } from '@iplik/i18n';
import { SectionCard, Select, SimpleGrid, Stack, Text, TextInput } from '@iplik/ui';
import { useReturnDraft } from './useReturnDraft';
import { useReturnSubmit } from './useReturnSubmit';
import { useReturnOptions } from './useReturnOptions';

const NUMBER_STYLES = { input: { textAlign: 'right' } } as const;

export function YarnReturnForm({ shipmentId }: { shipmentId: string }) {
  const { t } = useTranslation();
  const { draft, field, setYarnType, setLotId, reset } = useReturnDraft();
  const { yarns, lots, yarnType, lotId } = useReturnOptions(shipmentId, draft);
  const actions = useReturnSubmit(shipmentId, { ...draft, yarnType, lotId }, reset);
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
          <TextInput
            label={t('settlement.yarn.waybillNo')}
            maxLength={MAX_WAYBILL_NO_LENGTH}
            {...field('waybillNo')}
          />
        </SimpleGrid>
        <Text size="sm" c="dimmed">
          {t('settlement.yarn.returnNote')}
        </Text>
        <ReturnActions {...actions} />
      </Stack>
    </SectionCard>
  );
}
