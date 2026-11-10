import type { ShipmentDetail, Weighing } from '@iplik/api-client';

/** Görünümde olmayan alan için gösterilecek metnin i18n anahtarı. */
export type MissingKey = 'common.notRecorded';

/** Değer görünümde yoksa henüz kaydedilmemiştir; çıkış tartısı her rolün görünümündedir. */
export type FieldValue = { value: string } | { missing: MissingKey };

function present(value: string | number | undefined): FieldValue {
  return value === undefined ? { missing: 'common.notRecorded' } : { value: String(value) };
}

/** Deponun girdiği çıkış tartısı; kaydedilmediyse yoktur. */
export function findDispatchWeighing(shipment: ShipmentDetail): Weighing | undefined {
  return shipment.dispatchWeighing;
}

/** Çıkışta tartılan çuval sayısı. */
export function findBagCount(shipment: ShipmentDetail): FieldValue {
  return present(shipment.dispatchWeighing?.bagCount);
}

export function findDriverName(shipment: ShipmentDetail): FieldValue {
  return present(shipment.driverName);
}

/** Kalite itirazı sevkiyat tamamlandıktan (irsaliye yüklendikten) sonra açılır; asıl kontrol backend'dedir. */
export function isCompleted(shipment: ShipmentDetail): boolean {
  return shipment.status === 'COMPLETED';
}
