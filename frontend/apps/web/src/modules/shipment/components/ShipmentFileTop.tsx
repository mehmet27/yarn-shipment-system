import type { ShipmentDetail } from '@iplik/api-client';
import { EvidenceGallery, EvidenceUpload } from '@/modules/evidence';
import { useTranslation } from '@iplik/i18n';
import { ManagerDecisionNote } from './ManagerDecisionNote';
import { ShipmentFileActions } from './ShipmentFileActions';
import { ShipmentHeader } from './ShipmentHeader';
import { WeightCheckPanel } from './WeightCheckPanel';
import { CentralStockNotice } from '../stock/CentralStockNotice';
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
      {(shipment.status === 'DRAFT' || shipment.status === 'WEIGHT_MISMATCH') && (
        <CentralStockNotice
          alwaysShow
          demands={(shipment.lines ?? []).map((line) => ({
            productId: line.yarnTypeId,
            yarn: line.yarnTypeName ?? line.yarnTypeText ?? '',
            kg: line.netKg,
          }))}
        />
      )}
      <ManagerDecisionNote shipment={shipment} />
      <ShipmentFileActions
        shipmentId={shipment.shipmentId}
        shipmentDate={shipment.shipmentDate}
        subcontractorId={shipment.subcontractorId}
        driverName={shipment.driverName}
        plateNo={shipment.plateNo}
        actions={shipment.allowedActions}
      />
      <EvidenceUpload shipmentId={shipment.shipmentId} isDispatched={shipment.status !== 'DRAFT'} />
    </>
  );
}
