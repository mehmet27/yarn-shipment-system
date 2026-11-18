import { useTranslation } from '@iplik/i18n';
import { Button, Group, Paper, Stack, TextInput, Title } from '@iplik/ui';
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
    <Paper withBorder p="md" component="section" aria-label={t('settlement.fabric.title')}>
      <Stack>
        <Title order={3} size="h4">
          {t('settlement.fabric.title')}
        </Title>
        <FabricLimitNote limits={form.limits} />
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
            error={form.isOverLimit ? t('settlement.fabric.exceedsRemaining') : undefined}
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
    </Paper>
  );
}
