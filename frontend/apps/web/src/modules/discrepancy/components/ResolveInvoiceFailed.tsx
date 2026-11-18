import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Stack } from '@iplik/ui';
import { Link } from 'react-router';

/** Karar kaydedildi ama fatura yazılamadı: fatura, fasoncu dosyasındaki fire sekmesinden girilir. */
export function ResolveInvoiceFailed({
  shipmentId,
  onDone,
}: {
  shipmentId: string;
  onDone: () => void;
}) {
  const { t } = useTranslation();
  return (
    <Alert color="orange" role="alert">
      <Stack gap="xs">
        {t('discrepancy.resolve.invoiceFailed')}
        <Button
          component={Link}
          to={`/panel/sevkiyat/${shipmentId}`}
          variant="default"
          onClick={onDone}
        >
          {t('discrepancy.resolve.invoiceFailedOpen')}
        </Button>
        <Button variant="subtle" onClick={onDone}>
          {t('discrepancy.resolve.invoiceFailedDone')}
        </Button>
      </Stack>
    </Alert>
  );
}
