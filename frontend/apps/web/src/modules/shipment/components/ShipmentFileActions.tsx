import type { ShipmentAction } from '@iplik/api-client';
import { Group } from '@iplik/ui';
import { CompleteShipmentForm } from './CompleteShipmentForm';
import { UpdateShipmentDateForm } from './UpdateShipmentDateForm';
import { UpdateShipmentTransportForm } from './UpdateShipmentTransportForm';
import { WeightDecisionForm } from './WeightDecisionForm';

/** Sevkiyat dosyasındaki eylemler (iptal ve çıkış tartısı düğmesi hariç, onlar dosyanın başında ve sonunda); yalnızca `allowedActions` listesindekiler görünür (FE-SEC-03). */
export function ShipmentFileActions({
  shipmentId,
  shipmentDate,
  subcontractorId,
  driverName,
  plateNo,
  actions,
}: {
  shipmentId: string;
  shipmentDate: string;
  subcontractorId: string;
  driverName?: string | undefined;
  plateNo?: string | undefined;
  actions: readonly ShipmentAction[];
}) {
  const canEdit = actions.includes('UPDATE_SHIPMENT_DATE') || actions.includes('UPDATE_TRANSPORT');
  return (
    <>
      {actions.includes('RESOLVE_WEIGHT_MISMATCH') && (
        <WeightDecisionForm shipmentId={shipmentId} />
      )}
      {actions.includes('COMPLETE') && <CompleteShipmentForm shipmentId={shipmentId} />}
      {canEdit && (
        <Group gap="sm" align="flex-start">
          {actions.includes('UPDATE_SHIPMENT_DATE') && (
            <UpdateShipmentDateForm shipmentId={shipmentId} shipmentDate={shipmentDate} />
          )}
          {actions.includes('UPDATE_TRANSPORT') && (
            <UpdateShipmentTransportForm
              shipmentId={shipmentId}
              subcontractorId={subcontractorId}
              driverName={driverName}
              plateNo={plateNo}
            />
          )}
        </Group>
      )}
    </>
  );
}
