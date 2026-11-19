import type { FireSettlement } from '@iplik/api-client/settlements';
import { useTranslation } from '@iplik/i18n';
import { Alert, Anchor, Stack } from '@iplik/ui';
import { Link } from 'react-router';
import { usePermission } from '@/modules/auth';
import { CloseAction } from './CloseAction';
import { FireRateForm } from './FireRateForm';
import { formatKgText } from './format';

/**
 * Açık dosyada izne göre oran düzenleme, faturalanacak kilo bilgisi (kalan kilo varsa) ya da kapatma (kalmadıysa).
 * Fatura bu ekranda girilmez; yalnızca Faturalama ekranından (Faturalanacaklar sekmesi) girilir.
 */
export function FireOpenActions({ settlement }: { settlement: FireSettlement }) {
  const { t } = useTranslation();
  const canEditRate = usePermission('FIRE_RATE_EDIT');
  const canMark = usePermission('INVOICE_MARK');
  const hasFabric = Number(settlement.fabricKg) > 0;
  const hasFault = Number(settlement.remainingKg) > 0;
  const shouldShowInvoiceInfo = canMark && hasFabric && hasFault;
  const canClose = canMark && hasFabric && !hasFault;
  return (
    <Stack gap="lg">
      {canEditRate && <FireRateForm key={settlement.fireRate} settlement={settlement} />}
      {shouldShowInvoiceInfo && (
        <Alert color="blue" data-testid="invoice-info">
          {t('settlement.fire.invoiceInfoNote', { kg: formatKgText(settlement.remainingKg) })}{' '}
          <Anchor component={Link} to="/panel/faturalama">
            {t('settlement.fire.invoiceInfoLink')}
          </Anchor>
        </Alert>
      )}
      {canClose && <CloseAction shipmentId={settlement.shipmentId} />}
    </Stack>
  );
}
