import { useTranslation } from '@iplik/i18n';
import { Alert, SectionCard, SimpleGrid, Stack, Text, TextInput } from '@iplik/ui';
import { SaveConfirm } from '@/modules/shared';
import { MasterSelect } from '@/modules/shipment';
import { stockErrorKey } from '../stockErrors';
import { useReceiptSubmit } from '../useReceiptSubmit';
import { ReceiptLotFields } from './ReceiptLotFields';

/** Merkeze gelen ipliği stoğa yazar. */
export function YarnReceiptForm() {
  const { t } = useTranslation();
  const { draft, setDraft, field, error, submit, confirm, step, mutation } = useReceiptSubmit();
  return (
    <SectionCard title={t('stock.receipt.title')} icon="download">
      <Stack>
        <SimpleGrid cols={{ base: 1, sm: 2 }}>
          <MasterSelect
            kind="product"
            label={t('stock.receipt.yarnType')}
            isRequired
            value={draft.product}
            onChange={(product) => {
              setDraft((current) => ({ ...current, product }));
            }}
          />
          <TextInput
            label={t('stock.receipt.netKg')}
            inputMode="decimal"
            withAsterisk
            {...field('netKg')}
          />
        </SimpleGrid>
        <MasterSelect
          kind="supplier"
          label={t('stock.receipt.supplier')}
          isRequired
          value={draft.supplier}
          onChange={(supplier) => {
            setDraft((current) => ({ ...current, supplier }));
          }}
        />
        <Text size="xs" c="dimmed">
          {t('stock.receipt.yarnHint')}
        </Text>
        <ReceiptLotFields draft={draft} setDraft={setDraft} field={field} />
        <Text size="xs" c="dimmed">
          {t('stock.receipt.lotHint')}
        </Text>
        <TextInput label={t('stock.receipt.note')} {...field('note')} />
        {error !== undefined && <Alert color="red">{t(`stock.draftErrors.${error}`)}</Alert>}
        {mutation.isSuccess && error === undefined && (
          <Alert color="green">{t('stock.receipt.saved')}</Alert>
        )}
        {mutation.isError && <Alert color="red">{t(stockErrorKey(mutation.error))}</Alert>}
        <SaveConfirm
          isOpen={step.isOpen}
          isLoading={mutation.isPending}
          submitLabel={t('stock.receipt.submit')}
          onSubmit={submit}
          onConfirm={confirm}
          onCancel={step.close}
        />
      </Stack>
    </SectionCard>
  );
}
