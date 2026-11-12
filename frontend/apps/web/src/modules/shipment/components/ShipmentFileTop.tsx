import type { ShipmentDetail } from '@iplik/api-client';
import { EvidenceGallery, EvidenceUpload } from '@/modules/evidence';
import { useTranslation } from '@iplik/i18n';
import { ShipmentFileActions } from './ShipmentFileActions';
import { ShipmentHeader } from './ShipmentHeader';
import { WeightCheckPanel } from './WeightCheckPanel';
import { findWeightCheck, isCompleted } from './shipmentViews';

/** Dosyanın üstü: başlık, kantar uyuşmazlığı özeti, eylemler ve kanıt yükleme. */
export function ShipmentFileTop({ shipment }: { shipment: ShipmentDetail }) {
  const { t } = useTranslation();
  const weightCheck = findWeightCheck(shipment);
  return (
    <>
      <ShipmentHeader shipment={shipment} />
      {weightCheck !== undefined && shipment.status === 'WEIGHT_MISMATCH' && (
        <WeightCheckPanel check={weightCheck} />
      )}
      {isCompleted(shipment) && (
        <EvidenceGallery
          shipmentId={shipment.shipmentId}
          types={['WAYBILL']}
          title={t('evidence.view.waybill')}
          hideWhenEmpty
        />
      )}
      <ShipmentFileActions shipmentId={shipment.shipmentId} actions={shipment.allowedActions} />
      <EvidenceUpload shipmentId={shipment.shipmentId} />
    </>
  );
}
