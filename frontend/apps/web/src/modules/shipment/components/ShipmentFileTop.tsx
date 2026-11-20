import type { ShipmentDetail } from '@iplik/api-client';
import { EvidenceGallery, EvidenceUpload } from '@/modules/evidence';
import { useTranslation } from '@iplik/i18n';
import { Stack } from '@iplik/ui';
import { CancelShipmentForm } from './CancelShipmentForm';
import { ManagerDecisionNote } from './ManagerDecisionNote';
import { ShipmentFileActions } from './ShipmentFileActions';
import { ShipmentHeader } from './ShipmentHeader';
import { WeightCheckPanel } from './WeightCheckPanel';
import { CentralStockNotice } from '../stock/CentralStockNotice';
import { findWeightCheck, isCompleted } from './shipmentViews';

/** Yola çıkmış (yolda, tamamlanmış ya da iptal) sevkiyat durumları. */
const DEPARTED: readonly string[] = ['AWAITING_DELIVERY', 'COMPLETED', 'CANCELLED'];

/** Dosyanın üstü: başlık, kantar uyuşmazlığı özeti, eylemler ve kanıt yükleme. */
export function ShipmentFileTop({ shipment }: { shipment: ShipmentDetail }) {
  const { t } = useTranslation();
  const weightCheck = findWeightCheck(shipment);
  const actions = shipment.allowedActions;
  // Kantar uyuşmazlığında iptal, karar kartlarındadır; aynı işlem iki yerde durmaz.
  const canCancel = actions.includes('CANCEL') && !actions.includes('RESOLVE_WEIGHT_MISMATCH');
  const hasStockNotice = shipment.status === 'DRAFT' || shipment.status === 'WEIGHT_MISMATCH';
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
      <Stack gap="sm">
        {hasStockNotice && (
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
      </Stack>
      <ShipmentFileActions
        shipmentId={shipment.shipmentId}
        shipmentDate={shipment.shipmentDate}
        subcontractorId={shipment.subcontractorId}
        driverName={shipment.driverName}
        plateNo={shipment.plateNo}
        actions={actions}
      />
      <EvidenceUpload
        shipmentId={shipment.shipmentId}
        isDispatched={shipment.status !== 'DRAFT'}
        isDeparted={DEPARTED.includes(shipment.status)}
      />
      {canCancel && <CancelShipmentForm shipmentId={shipment.shipmentId} />}
    </>
  );
}
