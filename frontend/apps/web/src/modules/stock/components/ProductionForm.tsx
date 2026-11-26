import { useTranslation } from '@iplik/i18n';
import { Alert, Button, SectionCard, SimpleGrid, Stack, Text, TextInput } from '@iplik/ui';
import { ProductionFabricType } from './ProductionFabricType';
import { ProductionYarnRows } from './ProductionYarnRows';
import { productionRatioTotal } from '../stockDraft';
import { stockErrorKey } from '../stockErrors';
import { useProductionSubmit } from '../useProductionSubmit';

/** Merkez fabrikanın ürettiği kumaş: hangi iplikten kaç kg kumaş çıktığı girilir, iplik fire ile stoktan düşer. */
export function ProductionForm() {
  const { t } = useTranslation();
  const form = useProductionSubmit();
  const { draft, field, setYarn, addYarn, removeYarn, error, submit, mutation } = form;
  const total = productionRatioTotal(draft);
  return (
    <SectionCard title={t('stock.production.title')} icon="lots">
      <Stack>
        <Text size="sm" c="dimmed">
          {t('stock.production.hint')}
        </Text>
        <ProductionFabricType
          draft={draft}
          field={field}
          setFabricType={form.setFabricType}
          setYarns={form.setYarns}
        />
        <ProductionYarnRows
          draft={draft}
          onChange={setYarn}
          onAdd={addYarn}
          onRemove={removeYarn}
        />
        <Text
          size="sm"
          fw={600}
          c={total === 100 ? 'green' : 'red'}
          data-testid="central-ratio-total"
        >
          {t(total === 100 ? 'stock.production.ratioOk' : 'stock.production.ratioSum', {
            total: String(total).replace('.', ','),
          })}
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
