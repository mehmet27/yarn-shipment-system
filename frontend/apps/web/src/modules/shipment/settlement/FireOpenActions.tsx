import type { FireSettlement } from '@iplik/api-client/settlements';
import { useTranslation } from '@iplik/i18n';
import { Stack, Title } from '@iplik/ui';
import { usePermission } from '@/modules/auth';
import { CloseAction } from './CloseAction';
import { FireRateForm } from './FireRateForm';
import { InvoiceForm } from './InvoiceForm';

/** Açık dosyada izne göre oran düzenleme, fatura kaydı (hatalı kilo varsa) ya da kapatma (yoksa). */
export function FireOpenActions({ settlement }: { settlement: FireSettlement }) {
  const { t } = useTranslation();
  const canEditRate = usePermission('FIRE_RATE_EDIT');
  const canMark = usePermission('INVOICE_MARK');
  const hasFabric = Number(settlement.fabricKg) > 0;
  const hasFault = Number(settlement.faultyKg) > 0;
  const canInvoice = canMark && hasFabric && hasFault;
  const canClose = canMark && hasFabric && !hasFault;
  return (
    <Stack>
      {canEditRate && <FireRateForm key={settlement.fireRate} settlement={settlement} />}
      {canInvoice && (
        <Stack>
          <Title order={3} size="h4">
            {t('settlement.fire.invoiceTitle')}
          </Title>
          <InvoiceForm shipmentId={settlement.shipmentId} />
        </Stack>
      )}
      {canClose && <CloseAction shipmentId={settlement.shipmentId} />}
    </Stack>
  );
}
