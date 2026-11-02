import type { ShipmentAction } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Button } from '@iplik/ui';
import { Link } from 'react-router';
import { CancelShipmentForm } from './CancelShipmentForm';

/** Sevkiyat dosyasındaki eylemler; yalnızca `allowedActions` listesindekiler görünür (FE-SEC-03). */
export function ShipmentFileActions({
  shipmentId,
  actions,
}: {
  shipmentId: string;
  actions: readonly ShipmentAction[];
}) {
  const { t } = useTranslation();
  return (
    <>
      {actions.includes('RECORD_DISPATCH') && (
        <Button component={Link} to="cikis" relative="path" w="fit-content">
          {t('shipment.file.recordDispatch')}
        </Button>
      )}
      {actions.includes('CANCEL') && <CancelShipmentForm shipmentId={shipmentId} />}
    </>
  );
}
