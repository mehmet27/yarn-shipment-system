import type { ShipmentAction } from '@iplik/api-client';
import { Box, Group } from '@iplik/ui';
import { COMPLETE_SECTION_ID, WEIGHT_DECISION_SECTION_ID } from '../guide/guideSteps';
import { SECTION_SCROLL_MARGIN_PX } from '../guide/scrollToSection';
import { CompleteShipmentForm } from './CompleteShipmentForm';
import { UpdateShipmentDateForm } from './UpdateShipmentDateForm';
import { UpdateShipmentTransportForm } from './UpdateShipmentTransportForm';
import { WeightDecisionForm } from './WeightDecisionForm';

/** "Sonraki işlem" düğmesi bu bölümlere kaydırır; sabit üst çubuğun altında kalmasınlar. */
const SCROLL_TARGET = { scrollMarginTop: SECTION_SCROLL_MARGIN_PX } as const;

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
        <Box id={WEIGHT_DECISION_SECTION_ID} style={SCROLL_TARGET}>
          <WeightDecisionForm shipmentId={shipmentId} />
        </Box>
      )}
      {actions.includes('COMPLETE') && (
        <Box id={COMPLETE_SECTION_ID} style={SCROLL_TARGET}>
          <CompleteShipmentForm shipmentId={shipmentId} />
        </Box>
      )}
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
