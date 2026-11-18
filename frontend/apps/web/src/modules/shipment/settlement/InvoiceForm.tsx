import type { FireSettlement } from '@iplik/api-client/settlements';
import { useTranslation } from '@iplik/i18n';
import { Alert, SimpleGrid, Stack, Text, TextInput } from '@iplik/ui';
import { findRecipe } from '../components/shipmentViews';
import { useYarnLabel } from '../components/useYarnLabel';
import { useShipment } from '../hooks/useShipment';
import { yarnOptionsOf } from './fabricLimits';
import { InvoiceConfirm } from './InvoiceConfirm';
import { InvoiceRemaining } from './InvoiceRemaining';
import { InvoiceYarnRows } from './InvoiceYarnRows';
import { settlementErrorKey } from './settlementErrors';
import { useInvoiceDraft } from './useInvoiceDraft';

const yarnNames = (recipe: ReturnType<typeof findRecipe>) =>
  new Map((recipe === undefined ? [] : yarnOptionsOf(recipe.lines)).map((o) => [o.value, o.label]));

/**
 * "Fatura edildi" kaydı (INVOICE_MARK): fatura ERP'de kesilmiştir; müdür fatura no, tarih ve hangi iplikten kaç kilo
 * için fatura kesildiğini yazar. Yazılan kilo fasoncunun bakiyesinden düşer; kalan kilo bitmeden dosya açık kalır.
 */
export function InvoiceForm({ settlement }: { settlement: FireSettlement }) {
  const { t } = useTranslation();
  const yarnLabel = useYarnLabel();
  const shipment = useShipment(settlement.shipmentId).data;
  const recipe = shipment === undefined ? undefined : findRecipe(shipment);
  const names = yarnNames(recipe);
  const { fields, set, error, mutation, submit, confirmation } = useInvoiceDraft(settlement);
  return (
    <Stack>
      <InvoiceRemaining settlement={settlement} />
      <SimpleGrid cols={{ base: 1, sm: 2 }}>
        <TextInput
          label={t('settlement.fire.invoiceNo')}
          withAsterisk
          value={fields.invoiceNo}
          onChange={(event) => {
            set.invoiceNo(event.currentTarget.value);
          }}
        />
        <TextInput
          label={t('settlement.fire.invoicedOn')}
          type="date"
          withAsterisk
          value={fields.invoicedOn}
          onChange={(event) => {
            set.invoicedOn(event.currentTarget.value);
          }}
        />
      </SimpleGrid>
      {fields.lines.length > 0 && (
        <InvoiceYarnRows
          lines={fields.lines}
          labelOf={(yarn) => names.get(yarn) ?? yarnLabel(yarn)}
          onChange={set.line}
        />
      )}
      <TextInput
        label={t('settlement.fire.invoiceNote')}
        value={fields.note}
        onChange={(event) => {
          set.note(event.currentTarget.value);
        }}
      />
      <Text size="sm" c="dimmed">
        {t('settlement.fire.invoiceWarning')}
      </Text>
      {error !== undefined && <Alert color="red">{t(`settlement.draftErrors.${error}`)}</Alert>}
      {mutation.isError && <Alert color="red">{t(settlementErrorKey(mutation.error))}</Alert>}
      <InvoiceConfirm
        isOpen={confirmation.isOpen}
        loading={mutation.isPending}
        onSubmit={submit}
        onConfirm={confirmation.confirm}
        onCancel={confirmation.cancel}
      />
    </Stack>
  );
}
