import type { FireSettlementRow } from '@iplik/api-client/settlements';
import { useTranslation } from '@iplik/i18n';
import { Anchor, Box, Group, Stack, Text } from '@iplik/ui';
import { formatKgText, formatPercentText } from '@/modules/shipment';
import { Link } from 'react-router';
import { InvoiceAction } from './InvoiceAction';

/** Faturalanacaklar satırı: dosya özeti, faturası kesilen kilo ve "Fatura bilgisini gir". */
export function ToInvoiceRow({ row, isFirst }: { row: FireSettlementRow; isFirst: boolean }) {
  const { t } = useTranslation();
  return (
    <Box
      component="article"
      aria-label={row.shipmentNo}
      p="md"
      style={{ borderTop: isFirst ? undefined : '1px solid var(--app-line-soft)' }}
    >
      <Stack gap={4}>
        <Group justify="space-between" gap="xs">
          <Anchor component={Link} to={`/panel/sevkiyat/${row.shipmentId}`} fw={600}>
            {row.shipmentNo}
          </Anchor>
          <Text size="sm">{row.subcontractorTitle}</Text>
        </Group>
        <Text size="sm" c="dimmed">
          {t('settlement.toInvoice.sent')} {formatKgText(row.sentKg)} ·{' '}
          {t('settlement.toInvoice.fabric')} {formatKgText(row.fabricKg)} ·{' '}
          {t('settlement.toInvoice.expected')} {formatKgText(row.expectedFabricKg)}
        </Text>
        <Text fw={600}>
          {t('settlement.toInvoice.faulty')} {formatKgText(row.remainingKg)} ·{' '}
          {t('settlement.toInvoice.fire')} {formatPercentText(row.actualFirePercent)}
        </Text>
        {Number(row.invoicedKg) > 0 && (
          <Text size="sm" c="dimmed">
            {t('settlement.toInvoice.alreadyInvoiced', { kg: formatKgText(row.invoicedKg) })}
          </Text>
        )}
        <InvoiceAction shipmentId={row.shipmentId} shipmentNo={row.shipmentNo} />
      </Stack>
    </Box>
  );
}
