import type { ShipmentStatus } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { MiniProgress } from '@iplik/ui';
import { PROGRESS_TOTAL_STEPS, toProgressDone } from './progressSteps';
import { toStatusColor } from './statusColor';

export type ShipmentProgressProps = {
  status: ShipmentStatus;
};

/** Satırda sürecin kaçıncı adımda olduğunu gösteren küçük çubuk; metni ekran okuyucuya verir. */
export function ShipmentProgress({ status }: ShipmentProgressProps) {
  const { t } = useTranslation();
  const done = toProgressDone(status);
  return (
    <MiniProgress
      total={PROGRESS_TOTAL_STEPS}
      done={done}
      color={toStatusColor(status)}
      label={
        status === 'CANCELLED'
          ? t('shipment.progress.cancelled')
          : t('shipment.progress.label', { done, total: PROGRESS_TOTAL_STEPS })
      }
    />
  );
}
