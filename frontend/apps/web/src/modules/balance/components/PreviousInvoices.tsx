import type { FireSettlement } from '@iplik/api-client/settlements';
import { useTranslation } from '@iplik/i18n';
import { Stack, Text } from '@iplik/ui';
import { formatKgText } from '@/modules/shipment';

/** Aynı sevkiyata daha önce girilmiş faturalar (no · tarih · kilo); yenisini girmeden önce görünür. */
export function PreviousInvoices({ settlement }: { settlement: FireSettlement }) {
  const { t } = useTranslation();
  return (
    <Stack gap={4} data-testid="previous-invoices">
      <Text size="sm" fw={600}>
        {t('settlement.toInvoice.previousTitle')}
      </Text>
      {settlement.invoices.length === 0 ? (
        <Text size="sm" c="dimmed">
          {t('settlement.toInvoice.previousNone')}
        </Text>
      ) : (
        settlement.invoices.map((entry) => (
          <Text key={entry.invoiceNo} size="sm" c="dimmed">
            {t('settlement.fire.invoiceLine', {
              no: entry.invoiceNo,
              date: entry.invoicedOn,
              kg: formatKgText(entry.kg),
            })}
          </Text>
        ))
      )}
    </Stack>
  );
}
