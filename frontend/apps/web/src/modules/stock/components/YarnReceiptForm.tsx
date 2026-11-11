import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Paper, Stack, TextInput, Title } from '@iplik/ui';
import { useState } from 'react';
import { useRecordYarnReceipt } from '../hooks/useCentralStock';
import type { ReceiptDraft, StockDraftError } from '../stockDraft';
import { EMPTY_RECEIPT_DRAFT, toReceiptRequest } from '../stockDraft';
import { stockErrorKey } from '../stockErrors';

/** Merkeze gelen ipliği stoğa yazar. */
export function YarnReceiptForm() {
  const { t } = useTranslation();
  const [draft, setDraft] = useState<ReceiptDraft>(EMPTY_RECEIPT_DRAFT);
  const [error, setError] = useState<StockDraftError>();
  const mutation = useRecordYarnReceipt();
  const field = (name: keyof ReceiptDraft) => ({
    value: draft[name],
    onChange: (event: React.ChangeEvent<HTMLInputElement>) => {
      const { value } = event.currentTarget;
      setDraft((current) => ({ ...current, [name]: value }));
    },
  });
  const submit = () => {
    const request = toReceiptRequest(draft);
    setError(typeof request === 'string' ? request : undefined);
    if (typeof request !== 'string') {
      mutation.mutate(request, {
        onSuccess: () => {
          setDraft(EMPTY_RECEIPT_DRAFT);
        },
      });
    }
  };
  return (
    <Paper withBorder p="md" component="section" aria-label={t('stock.receipt.title')}>
      <Stack>
        <Title order={3} size="h4">
          {t('stock.receipt.title')}
        </Title>
        <TextInput label={t('stock.receipt.yarnType')} withAsterisk {...field('yarnType')} />
        <TextInput
          label={t('stock.receipt.netKg')}
          inputMode="decimal"
          withAsterisk
          {...field('netKg')}
        />
        <TextInput label={t('stock.receipt.note')} {...field('note')} />
        {error !== undefined && <Alert color="red">{t(`stock.draftErrors.${error}`)}</Alert>}
        {mutation.isError && <Alert color="red">{t(stockErrorKey(mutation.error))}</Alert>}
        <Button w="fit-content" loading={mutation.isPending} onClick={submit}>
          {t('stock.receipt.submit')}
        </Button>
      </Stack>
    </Paper>
  );
}
