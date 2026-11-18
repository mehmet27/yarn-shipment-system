import { useTranslation } from '@iplik/i18n';
import { SimpleGrid, Stack, Text, TextInput } from '@iplik/ui';

export type InvoiceDraft = { invoiceNo: string; invoicedOn: string; note: string };

/** Bugünün tarihi (İstanbul saatiyle), `YYYY-MM-DD`; İsveççe yerel ayar bu biçimi verir. */
const DATE_FORMAT = new Intl.DateTimeFormat('sv-SE', { timeZone: 'Europe/Istanbul' });
export const emptyInvoice = (): InvoiceDraft => ({
  invoiceNo: '',
  invoicedOn: DATE_FORMAT.format(new Date()),
  note: '',
});

const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;
export const isInvoiceComplete = (draft: InvoiceDraft) =>
  draft.invoiceNo.trim() !== '' && DATE_PATTERN.test(draft.invoicedOn);

export type ResolveInvoiceFieldsProps = {
  value: InvoiceDraft;
  onChange: (value: InvoiceDraft) => void;
};

/** "Haklı, faturalandı" kararında fatura numarası, tarihi ve isteğe bağlı not. */
export function ResolveInvoiceFields({ value, onChange }: ResolveInvoiceFieldsProps) {
  const { t } = useTranslation();
  return (
    <Stack>
      <SimpleGrid cols={{ base: 1, sm: 2 }}>
        <TextInput
          label={t('settlement.fire.invoiceNo')}
          withAsterisk
          value={value.invoiceNo}
          onChange={(event) => {
            onChange({ ...value, invoiceNo: event.currentTarget.value });
          }}
        />
        <TextInput
          label={t('settlement.fire.invoicedOn')}
          type="date"
          withAsterisk
          value={value.invoicedOn}
          onChange={(event) => {
            onChange({ ...value, invoicedOn: event.currentTarget.value });
          }}
        />
      </SimpleGrid>
      <TextInput
        label={t('settlement.fire.invoiceNote')}
        value={value.note}
        onChange={(event) => {
          onChange({ ...value, note: event.currentTarget.value });
        }}
      />
      <Text size="sm" c="dimmed">
        {t('settlement.fire.invoiceWarning')}
      </Text>
    </Stack>
  );
}
