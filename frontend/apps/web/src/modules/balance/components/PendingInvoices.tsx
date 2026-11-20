import { useTranslation } from '@iplik/i18n';
import { Button, Icon, SectionCard, Stack } from '@iplik/ui';
import { PendingRow } from '@/modules/shipment';
import { Link } from 'react-router';
import { usePendingInvoices } from '../hooks/usePendingSettlements';

/**
 * Müdürün ana sayfadaki iş listesi: faturası beklenen fire dosyaları. Fatura yalnızca Faturalama ekranından girilir;
 * kart oraya götürür. Bekleyen yoksa hiçbir şey çizmez.
 */
export function PendingInvoices() {
  const { t } = useTranslation();
  const pending = usePendingInvoices();
  if (pending.items.length === 0) return null;
  return (
    <SectionCard
      title={t('settlement.toInvoice.pendingTitle', { count: pending.total })}
      description={t('settlement.toInvoice.pendingHint')}
      icon="invoices"
      testId="pending-invoices"
    >
      <Stack gap="sm" align="flex-start">
        <Stack gap={0}>
          {pending.items.map((item) => (
            <PendingRow
              key={item.shipmentId}
              to={`/panel/sevkiyat/${item.shipmentId}?sekme=fire`}
              shipmentNo={item.shipmentNo}
              description={item.subcontractorTitle}
              color="yellow"
            />
          ))}
        </Stack>
        <Button
          component={Link}
          to="/panel/faturalama"
          variant="default"
          rightSection={<Icon name="arrowRight" size={16} />}
        >
          {t('settlement.toInvoice.pendingOpen')}
        </Button>
      </Stack>
    </SectionCard>
  );
}
