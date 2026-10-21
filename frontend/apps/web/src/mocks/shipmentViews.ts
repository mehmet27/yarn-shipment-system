import type {
  DriverShipmentView,
  InternalShipmentView,
  Role,
  ShipmentDetail,
  ShipmentSummary,
  SubcontractorShipmentView,
  WarehouseShipmentView,
} from '@iplik/api-client';

/** Her görünümde bulunan ortak alanlar. */
function toBase(shipment: InternalShipmentView) {
  const { shipmentId, shipmentNo, siteId, status, subcontractorTitle, createdAt, allowedActions } =
    shipment;
  return {
    shipmentId,
    shipmentNo,
    siteId,
    status,
    subcontractorTitle,
    createdAt,
    allowedActions,
    ...(shipment.plateNo === undefined ? {} : { plateNo: shipment.plateNo }),
  };
}

function toWarehouseView(shipment: InternalShipmentView): WarehouseShipmentView {
  const { deliveryWeighing, deliverySealCondition, lockedAt, ...rest } = shipment;
  return { ...rest, audience: 'WAREHOUSE' };
}

function toSubcontractorView(shipment: InternalShipmentView): SubcontractorShipmentView {
  const { driverName, deliveryWeighing, deliverySealCondition } = shipment;
  return {
    ...toBase(shipment),
    audience: 'SUBCONTRACTOR',
    ...(driverName === undefined ? {} : { driverName }),
    ...(deliveryWeighing === undefined ? {} : { deliveryWeighing }),
    ...(deliverySealCondition === undefined ? {} : { deliverySealCondition }),
  };
}

function toDriverView(shipment: InternalShipmentView): DriverShipmentView {
  const { dispatchWeighing, dispatchSealNo } = shipment;
  return {
    ...toBase(shipment),
    audience: 'DRIVER',
    ...(dispatchWeighing === undefined ? {} : { dispatchBagCount: dispatchWeighing.bagCount }),
    ...(dispatchSealNo === undefined ? {} : { dispatchSealNo }),
  };
}

/** Backend'in role göre görünüm seçimini taklit eder (SEC-05): kör alanlar görünümde hiç yoktur. */
export function toDetailView(shipment: InternalShipmentView, role: Role): ShipmentDetail {
  switch (role) {
    case 'WAREHOUSE':
      return toWarehouseView(shipment);
    case 'SUBCONTRACTOR':
      return toSubcontractorView(shipment);
    case 'DRIVER':
      return toDriverView(shipment);
    case 'ADMIN':
    case 'QUALITY':
    case 'MANAGER':
      return shipment;
  }
}

/** Liste satırı her rol için aynıdır; beklenen yük listede yoktur. */
export function toSummary(shipment: InternalShipmentView): ShipmentSummary {
  return {
    ...toBase(shipment),
    subcontractorId: shipment.subcontractorId,
    ...(shipment.driverName === undefined ? {} : { driverName: shipment.driverName }),
  };
}
