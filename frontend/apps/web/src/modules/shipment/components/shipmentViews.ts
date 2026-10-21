import type { ShipmentDetail, Weighing } from '@iplik/api-client';

export type ShipmentWeighings = {
  dispatchWeighing?: Weighing;
  deliveryWeighing?: Weighing;
};

/** Görünüm türüne göre var olan tartım alanları; kör alanlar görünümde hiç yoktur. */
export function toWeighings(shipment: ShipmentDetail): ShipmentWeighings {
  switch (shipment.audience) {
    case 'INTERNAL':
      return pickDefined({
        dispatchWeighing: shipment.dispatchWeighing,
        deliveryWeighing: shipment.deliveryWeighing,
      });
    case 'WAREHOUSE':
      return pickDefined({ dispatchWeighing: shipment.dispatchWeighing });
    case 'SUBCONTRACTOR':
      return pickDefined({ deliveryWeighing: shipment.deliveryWeighing });
    case 'DRIVER':
      return {};
  }
}

export function findSealNo(shipment: ShipmentDetail): string | undefined {
  return 'dispatchSealNo' in shipment ? shipment.dispatchSealNo : undefined;
}

export function findDriverName(shipment: ShipmentDetail): string | undefined {
  return 'driverName' in shipment ? shipment.driverName : undefined;
}

/** exactOptionalPropertyTypes: undefined değerli alanlar nesneye yazılmaz. */
function pickDefined({
  dispatchWeighing,
  deliveryWeighing,
}: {
  dispatchWeighing?: Weighing | undefined;
  deliveryWeighing?: Weighing | undefined;
}): ShipmentWeighings {
  return {
    ...(dispatchWeighing === undefined ? {} : { dispatchWeighing }),
    ...(deliveryWeighing === undefined ? {} : { deliveryWeighing }),
  };
}
