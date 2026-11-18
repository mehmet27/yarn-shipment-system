import { useTranslation } from '@iplik/i18n';
import { Button, SectionCard, SimpleGrid, Stack, TextInput } from '@iplik/ui';
import { FabricConfirm } from './FabricConfirm';
import { FabricLimitNote } from './FabricLimitNote';
import { FabricMessages } from './FabricMessages';
import { FabricYarnRows } from './FabricYarnRows';
import { useFabricSubmit } from './useFabricSubmit';

/** Kumaş girişi formu; iplik dağılımı sevkiyattaki ipliklerden seçilir, toplamı kumaş kilosuna eşit olmalı. */
export function FabricForm({ shipmentId }: { shipmentId: string }) {
  const { t } = useTranslation();
  const form = useFabricSubmit(shipmentId);
  const { draft, field, mutation, request } = form;
  return (
    <SectionCard title={t('settlement.fabric.title')}>
      <Stack>
        <FabricLimitNote limits={form.limits} />
        <SimpleGrid cols={{ base: 1, sm: 2 }}>
          <TextInput
            label={t('settlement.fabric.fabricType')}
            withAsterisk
            {...field('fabricType')}
          />
          <TextInput
            label={t('settlement.fabric.waybillNo')}
            withAsterisk
            {...field('waybillNo')}
          />
          <TextInput
            label={t('settlement.fabric.netKg')}
            inputMode="decimal"
            withAsterisk
            error={form.isOverLimit ? t('settlement.fabric.exceedsRemaining') : undefined}
            {...field('netKg')}
          />
          <TextInput
            label={t('settlement.fabric.rollCount')}
            inputMode="numeric"
            {...field('rollCount')}
          />
        </SimpleGrid>
        <FabricYarnRows
          rows={draft.yarns}
          yarnNames={form.limits.yarnNames}
          onChange={form.setYarn}
          onAdd={form.addYarn}
          onRemove={form.removeYarn}
        />
        <FabricMessages form={form} />
        {form.isConfirming && typeof request !== 'string' ? (
          <FabricConfirm
            fabricType={request.fabricType}
            netKg={request.netKg}
            loading={mutation.isPending}
            onConfirm={form.confirm}
            onCancel={form.cancel}
          />
        ) : (
          <Button w="fit-content" onClick={form.ask}>
            {t('settlement.fabric.submit')}
          </Button>
        )}
      </Stack>
    </SectionCard>
  );
}
