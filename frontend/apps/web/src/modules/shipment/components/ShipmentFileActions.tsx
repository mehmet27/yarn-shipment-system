import type { ShipmentAction } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { ActionBar, Button, Text } from '@iplik/ui';
import { Link } from 'react-router';
import { useShipmentBags } from '../hooks/useShipmentBags';
import { CancelShipmentForm } from './CancelShipmentForm';
import { CompleteShipmentForm } from './CompleteShipmentForm';
import { UpdateShipmentDateForm } from './UpdateShipmentDateForm';
import { WeightDecisionForm } from './WeightDecisionForm';

/** Sevkiyat dosyasındaki eylemler; yalnızca `allowedActions` listesindekiler görünür (FE-SEC-03). */
export function ShipmentFileActions({
  shipmentId,
  shipmentDate,
  actions,
}: {
  shipmentId: string;
  shipmentDate: string;
  actions: readonly ShipmentAction[];
}) {
  const { t } = useTranslation();
  const canRecordDispatch = actions.includes('RECORD_DISPATCH');
  // Çuval eklenmeden tartı girilemez (çuval sayısı eşleşmeli); liste yüklenene kadar düğme açık kalır.
  const bags = useShipmentBags(shipmentId, canRecordDispatch);
  const isBagsFirst = bags.data?.dispatchBags.length === 0;
  return (
    <>
      {canRecordDispatch && (
        <ActionBar>
          {isBagsFirst && (
            <Text size="sm" c="dimmed">
              {t('shipment.file.bagsFirst')}
            </Text>
          )}
          {!isBagsFirst && (
            <Button component={Link} to="cikis" relative="path">
              {t('shipment.file.recordDispatch')}
            </Button>
          )}
        </ActionBar>
      )}
      {actions.includes('RESOLVE_WEIGHT_MISMATCH') && (
        <WeightDecisionForm shipmentId={shipmentId} />
      )}
      {actions.includes('COMPLETE') && <CompleteShipmentForm shipmentId={shipmentId} />}
      {actions.includes('UPDATE_SHIPMENT_DATE') && (
        <UpdateShipmentDateForm shipmentId={shipmentId} shipmentDate={shipmentDate} />
      )}
      {actions.includes('CANCEL') && <CancelShipmentForm shipmentId={shipmentId} />}
    </>
  );
}
