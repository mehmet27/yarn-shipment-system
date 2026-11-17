import type { InternalShipmentView, WarehouseShipmentView } from '@iplik/api-client';
import { formatDate, useTranslation } from '@iplik/i18n';
import { Alert, SectionCard, Stack } from '@iplik/ui';
import { DispatchForm } from './DispatchForm';

type Shipment = InternalShipmentView | WarehouseShipmentView;

function closedMessageKey(shipment: Shipment): 'shipment.notDue' | 'dispatch.notDraft' {
  return shipment.status === 'DRAFT' ? 'shipment.notDue' : 'dispatch.notDraft';
}

/** Form yalnızca `RECORD_DISPATCH` verildiyse (kantar uyuşmazlığında değil) açılır; değilse nedeni söylenir. */
export function DispatchPanel({ shipment, onSaved }: { shipment: Shipment; onSaved: () => void }) {
  const { t } = useTranslation();
  const isOpen =
    shipment.allowedActions.includes('RECORD_DISPATCH') && shipment.status !== 'WEIGHT_MISMATCH';
  if (!isOpen) {
    return (
      <Alert color="yellow">
        {t(closedMessageKey(shipment), { date: formatDate(shipment.shipmentDate) })}
      </Alert>
    );
  }
  const planned =
    shipment.plannedNetKg === undefined ? {} : { plannedNetKg: shipment.plannedNetKg };
  return (
    <Stack maw={560}>
      <SectionCard title={t('dispatch.title')} icon="truck">
        <DispatchForm shipmentId={shipment.shipmentId} {...planned} onSaved={onSaved} />
      </SectionCard>
    </Stack>
  );
}
