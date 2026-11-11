import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Stack, Text, TextInput } from '@iplik/ui';
import { useState } from 'react';
import { useMarkInvoiced } from './hooks';
import type { DraftError } from './settlementDraft';
import { toInvoiceRequest } from './settlementDraft';
import { settlementErrorKey } from './settlementErrors';

/** Bugünün tarihi (İstanbul saatiyle), `YYYY-MM-DD`; İsveççe yerel ayar bu biçimi verir. */
const DATE_FORMAT = new Intl.DateTimeFormat('sv-SE', { timeZone: 'Europe/Istanbul' });
const todayIso = () => DATE_FORMAT.format(new Date());

/** "Fatura edildi" kaydı (INVOICE_MARK): yalnız hatalı kilosu olan dosyada; geri alınamaz. */
export function InvoiceForm({ shipmentId }: { shipmentId: string }) {
  const { t } = useTranslation();
  const [invoiceNo, setInvoiceNo] = useState('');
  const [invoicedOn, setInvoicedOn] = useState(todayIso);
  const [note, setNote] = useState('');
  const [error, setError] = useState<DraftError>();
  const mutation = useMarkInvoiced(shipmentId);
  const submit = () => {
    const request = toInvoiceRequest(invoiceNo, invoicedOn, note);
    setError(typeof request === 'string' ? request : undefined);
    if (typeof request !== 'string') mutation.mutate(request);
  };
  return (
    <Stack>
      <Group grow align="flex-start">
        <TextInput
          label={t('settlement.fire.invoiceNo')}
          withAsterisk
          value={invoiceNo}
          onChange={(event) => {
            setInvoiceNo(event.currentTarget.value);
          }}
        />
        <TextInput
          label={t('settlement.fire.invoicedOn')}
          type="date"
          withAsterisk
          value={invoicedOn}
          onChange={(event) => {
            setInvoicedOn(event.currentTarget.value);
          }}
        />
      </Group>
      <TextInput
        label={t('settlement.fire.invoiceNote')}
        value={note}
        onChange={(event) => {
          setNote(event.currentTarget.value);
        }}
      />
      <Text size="sm" c="dimmed">
        {t('settlement.fire.invoiceWarning')}
      </Text>
      {error !== undefined && <Alert color="red">{t(`settlement.draftErrors.${error}`)}</Alert>}
      {mutation.isError && <Alert color="red">{t(settlementErrorKey(mutation.error))}</Alert>}
      <Button w="fit-content" loading={mutation.isPending} onClick={submit}>
        {t('settlement.fire.invoiceSubmit')}
      </Button>
    </Stack>
  );
}
