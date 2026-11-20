import type { ShipmentDetail } from '@iplik/api-client';
import { NextStepCard } from './NextStepCard';
import { ShipmentSteps } from './ShipmentSteps';
import { useShipmentGuide } from './useShipmentGuide';

/**
 * Sevkiyat dosyasının yol göstericisi: sürecin adımları ve "sıradaki adım" kutusu. İptal edilen sevkiyatta adım
 * çubuğu çizilmez; çuval listesi ya da fire dosyası yüklenirken kutu bekler (yanlış adım bir an bile görünmesin).
 */
export function ShipmentGuide({ shipment }: { shipment: ShipmentDetail }) {
  const guide = useShipmentGuide(shipment);
  if (guide === undefined) return null;
  return (
    <>
      {guide.task !== 'CANCELLED' && <ShipmentSteps stepIndex={guide.stepIndex} />}
      <NextStepCard guide={guide} />
    </>
  );
}
