import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Paper, Stack, Text, TextInput, Title } from '@iplik/ui';
import { useState } from 'react';
import { useFireRateDefault } from '@/modules/shipment';
import { ProductionYarnRows } from './ProductionYarnRows';
import { useRecordCentralProduction } from '../hooks/useCentralStock';
import type { StockDraftError } from '../stockDraft';
import { productionTotalKg, toProductionRequest } from '../stockDraft';
import { stockErrorKey } from '../stockErrors';
import { useProductionDraft } from '../useProductionDraft';

/** Merkez fabrikanın ürettiği kumaş: hangi iplikten kaç kg kumaş çıktığı girilir, iplik fire ile stoktan düşer. */
export function ProductionForm() {
  const { t } = useTranslation();
  const defaultFirePct = useFireRateDefault();
  const { draft, field, setYarn, addYarn, removeYarn, reset } = useProductionDraft(defaultFirePct);
  const [error, setError] = useState<StockDraftError>();
  const mutation = useRecordCentralProduction();
  const submit = () => {
    const request = toProductionRequest(draft);
    setError(typeof request === 'string' ? request : undefined);
    if (typeof request !== 'string') mutation.mutate(request, { onSuccess: reset });
  };
  return (
    <Paper withBorder p="md" component="section" aria-label={t('stock.production.title')}>
      <Stack>
        <Title order={3} size="h4">
          {t('stock.production.title')}
        </Title>
        <Text size="sm" c="dimmed">
          {t('stock.production.hint')}
        </Text>
        <TextInput label={t('stock.production.fabricType')} withAsterisk {...field('fabricType')} />
        <ProductionYarnRows
          rows={draft.yarns}
          onChange={setYarn}
          onAdd={addYarn}
          onRemove={removeYarn}
        />
        <Text size="sm">{t('stock.production.total', { kg: productionTotalKg(draft) })}</Text>
        <Group grow align="flex-start">
          <TextInput
            label={t('stock.production.firePct')}
            inputMode="decimal"
            withAsterisk
            {...field('firePct')}
          />
          <TextInput
            label={t('stock.production.rollCount')}
            inputMode="numeric"
            {...field('rollCount')}
          />
          <TextInput label={t('stock.production.waybillNo')} {...field('waybillNo')} />
        </Group>
        {error !== undefined && <Alert color="red">{t(`stock.draftErrors.${error}`)}</Alert>}
        {mutation.isError && <Alert color="red">{t(stockErrorKey(mutation.error))}</Alert>}
        <Button w="fit-content" loading={mutation.isPending} onClick={submit}>
          {t('stock.production.submit')}
        </Button>
      </Stack>
    </Paper>
  );
}
