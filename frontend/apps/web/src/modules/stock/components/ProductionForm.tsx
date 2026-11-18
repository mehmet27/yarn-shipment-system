import { useTranslation } from '@iplik/i18n';
import { Alert, Button, SectionCard, SimpleGrid, Stack, Text, TextInput } from '@iplik/ui';
import { formatKgText } from '@iplik/units';
import { ProductionYarnRows } from './ProductionYarnRows';
import { productionTotalKg } from '../stockDraft';
import { stockErrorKey } from '../stockErrors';
import { useProductionSubmit } from '../useProductionSubmit';

/** Merkez fabrikanın ürettiği kumaş: hangi iplikten kaç kg kumaş çıktığı girilir, iplik fire ile stoktan düşer. */
export function ProductionForm() {
  const { t } = useTranslation();
  const { draft, field, setYarn, addYarn, removeYarn, error, submit, mutation } =
    useProductionSubmit();
  return (
    <SectionCard title={t('stock.production.title')} icon="lots">
      <Stack>
        <Text size="sm" c="dimmed">
          {t('stock.production.hint')}
        </Text>
        <SimpleGrid cols={{ base: 1, sm: 3 }}>
          <TextInput
            label={t('stock.production.fabricType')}
            withAsterisk
            {...field('fabricType')}
          />
        </SimpleGrid>
        <ProductionYarnRows
          rows={draft.yarns}
          onChange={setYarn}
          onAdd={addYarn}
          onRemove={removeYarn}
        />
        <Text size="sm">
          {t('stock.production.total', { kg: formatKgText(productionTotalKg(draft)) })}
        </Text>
        <SimpleGrid cols={{ base: 1, sm: 3 }}>
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
        </SimpleGrid>
        {error !== undefined && <Alert color="red">{t(`stock.draftErrors.${error}`)}</Alert>}
        {mutation.isSuccess && error === undefined && (
          <Alert color="green">{t('stock.production.saved')}</Alert>
        )}
        {mutation.isError && <Alert color="red">{t(stockErrorKey(mutation.error))}</Alert>}
        <Button w="fit-content" loading={mutation.isPending} onClick={submit}>
          {t('stock.production.submit')}
        </Button>
      </Stack>
    </SectionCard>
  );
}
