import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Paper, Stack, Text, TextInput, Title } from '@iplik/ui';
import { MasterSelect } from '@/modules/shipment';
import { stockErrorKey } from '../stockErrors';
import { useReceiptSubmit } from '../useReceiptSubmit';

/** Merkeze gelen ipliği stoğa yazar. */
export function YarnReceiptForm() {
  const { t } = useTranslation();
  const { draft, setDraft, field, error, submit, mutation } = useReceiptSubmit();
  return (
    <Paper withBorder p="md" component="section" aria-label={t('stock.receipt.title')}>
      <Stack>
        <Title order={3} size="h4">
          {t('stock.receipt.title')}
        </Title>
        <MasterSelect
          kind="product"
          label={t('stock.receipt.yarnType')}
          isRequired
          value={draft.product}
          onChange={(product) => {
            setDraft((current) => ({ ...current, product }));
          }}
        />
        <Text size="xs" c="dimmed">
          {t('stock.receipt.yarnHint')}
        </Text>
        <TextInput
          label={t('stock.receipt.netKg')}
          inputMode="decimal"
          withAsterisk
          {...field('netKg')}
        />
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
    </Paper>
  );
}
