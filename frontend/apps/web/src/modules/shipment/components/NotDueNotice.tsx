import type { ShipmentDetail } from '@iplik/api-client';
import { formatDate, todayIsoDate, useTranslation } from '@iplik/i18n';
import { Alert } from '@iplik/ui';

type NotDueNoticeProps = { shipment: ShipmentDetail };

/** Sevkiyat günü gelmediyse depoyu bilgilendirir; gün gelene kadar çuval ve çıkış tartısı kapalıdır. */
export function NotDueNotice({ shipment }: NotDueNoticeProps) {
  const { t } = useTranslation();
  if (shipment.status !== 'DRAFT' || shipment.shipmentDate <= todayIsoDate()) return null;
  return (
    <Alert color="yellow">
      {t('shipment.notDue', { date: formatDate(shipment.shipmentDate) })}
    </Alert>
  );
}
