import { useTranslation } from '@iplik/i18n';
import { Button, SectionCard, SimpleGrid, Stack, Text, TextInput } from '@iplik/ui';
import { FabricTypeSelect } from '../create/FabricTypeSelect';
import { MAX_WAYBILL_NO_LENGTH } from '../waybill';
import { FabricMessages } from './FabricMessages';
import { FabricPreview } from './FabricPreview';
import { FabricYarnRows } from './FabricYarnRows';
import { useFabricSubmit } from './useFabricSubmit';

const NUMBER_STYLES = { input: { textAlign: 'right' } } as const;

/**
 * Kumaş girişi formu: fasoncudaki her iplik için kumaşın yüzde kaçı olduğu yazılır (toplam 100). Kaydetmeden önce
 * hangi sevkiyattan kaç kilo düşeceği gösterilir.
 */
export function FabricForm({ shipmentId }: { shipmentId: string }) {
  const { t } = useTranslation();
  const form = useFabricSubmit(shipmentId);
  const { draft, field, mutation, request, checked } = form;
  return (
    <SectionCard title={t('settlement.fabric.title')}>
      <Stack>
        <SimpleGrid cols={{ base: 1, sm: 2 }}>
          <FabricTypeSelect
            label={t('settlement.fabric.fabricType')}
            value={draft.fabricType}
            onChange={form.changeFabricType}
            isRequired
          />
          <TextInput
            label={t('settlement.fabric.waybillNo')}
            withAsterisk
            maxLength={MAX_WAYBILL_NO_LENGTH}
            {...field('waybillNo')}
          />
          <TextInput
            label={t('settlement.fabric.netKg')}
            inputMode="decimal"
            styles={NUMBER_STYLES}
            withAsterisk
            {...field('netKg')}
          />
          <TextInput
            label={t('settlement.fabric.rollCount')}
            inputMode="numeric"
            styles={NUMBER_STYLES}
            {...field('rollCount')}
          />
        </SimpleGrid>
        {form.missingYarns.length > 0 && (
          <Text size="sm" c="orange">
            {t('settlement.fabric.recipeMissing', { yarns: form.missingYarns.join(', ') })}
          </Text>
        )}
        <FabricYarnRows options={form.options} ratios={draft.ratios} onChange={form.setRatio} />
        <FabricMessages form={form} />
        {checked !== undefined && typeof request !== 'string' ? (
          <FabricPreview
            preview={checked}
            fabricType={request.fabricType}
            netKg={request.netKg}
            isLoading={mutation.isPending}
            onConfirm={form.confirm}
            onCancel={form.cancel}
          />
        ) : (
          <Button w="fit-content" loading={form.isChecking} onClick={form.ask}>
            {t('settlement.fabric.submit')}
          </Button>
        )}
      </Stack>
    </SectionCard>
  );
}
