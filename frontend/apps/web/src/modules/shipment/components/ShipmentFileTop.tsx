import type { ShipmentDetail } from '@iplik/api-client';
import { EvidenceUpload } from '@/modules/evidence';
import { ShipmentFileActions } from './ShipmentFileActions';
import { ShipmentHeader } from './ShipmentHeader';
import { WeightCheckPanel } from './WeightCheckPanel';
import { findWeightCheck } from './shipmentViews';

/** Dosyanın üstü: başlık, kantar uyuşmazlığı özeti, eylemler ve kanıt yükleme. */
export function ShipmentFileTop({ shipment }: { shipment: ShipmentDetail }) {
  const weightCheck = findWeightCheck(shipment);
  return (
    <>
      <ShipmentHeader shipment={shipment} />
      {weightCheck !== undefined && shipment.status === 'WEIGHT_MISMATCH' && (
        <WeightCheckPanel check={weightCheck} />
      )}
      <ShipmentFileActions shipmentId={shipment.shipmentId} actions={shipment.allowedActions} />
      <EvidenceUpload shipmentId={shipment.shipmentId} />
    </>
  );
}
