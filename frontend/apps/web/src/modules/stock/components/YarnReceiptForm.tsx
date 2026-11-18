import { useTranslation } from '@iplik/i18n';
import { Alert, Button, SectionCard, SimpleGrid, Stack, Text, TextInput } from '@iplik/ui';
import { MasterSelect } from '@/modules/shipment';
import { stockErrorKey } from '../stockErrors';
import { useReceiptSubmit } from '../useReceiptSubmit';

/** Merkeze gelen ipliği stoğa yazar. */
export function YarnReceiptForm() {
  const { t } = useTranslation();
  const { draft, setDraft, field, error, submit, mutation } = useReceiptSubmit();
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
        <Text size="xs" c="dimmed">
          {t('stock.receipt.yarnHint')}
        </Text>
        <TextInput label={t('stock.receipt.note')} {...field('note')} />
        {error !== undefined && <Alert color="red">{t(`stock.draftErrors.${error}`)}</Alert>}
        {mutation.isSuccess && error === undefined && (
          <Alert color="green">{t('stock.receipt.saved')}</Alert>
        )}
        {mutation.isError && <Alert color="red">{t(stockErrorKey(mutation.error))}</Alert>}
        <Button w="fit-content" loading={mutation.isPending} onClick={submit}>
          {t('stock.receipt.submit')}
        </Button>
      </Stack>
    </SectionCard>
  );
}
