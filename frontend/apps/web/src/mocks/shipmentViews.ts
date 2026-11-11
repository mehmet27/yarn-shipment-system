import type {
  CurrentUser,
  InternalShipmentView,
  Permission,
  Role,
  ShipmentAction,
  ShipmentDetail,
  ShipmentSummary,
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
  return { ...shipment, audience: 'WAREHOUSE' };
}

/** Backend'in role göre görünüm seçimini taklit eder (SEC-05): kör alanlar görünümde hiç yoktur. */
export function toDetailView(shipment: InternalShipmentView, role: Role): ShipmentDetail {
  switch (role) {
    case 'WAREHOUSE':
      return toWarehouseView(shipment);
    case 'ADMIN':
    case 'QUALITY':
    case 'MANAGER':
    case 'GENERAL_MANAGER':
      return shipment;
  }
}

/** ShipmentAction'daki izin eşlemesi (backend ShipmentAccessPolicy). */
const ACTION_PERMISSION: Readonly<Record<ShipmentAction, Permission>> = {
  ASSIGN_BAGS: 'DISPATCH_RECORD',
  RECORD_DISPATCH: 'DISPATCH_RECORD',
  CANCEL: 'SHIPMENT_CANCEL',
  COMPLETE: 'SHIPMENT_COMPLETE',
  OPEN_QUALITY_CLAIM: 'QUALITY_CLAIM_OPEN',
  CREATE_CORRECTION: 'CORRECTION_CREATE',
};

/** Backend gibi işlemi yalnızca izni olan kullanıcıya açar; Yönetici hiç düzenleme butonu görmez. */
export function toPermittedView(view: ShipmentDetail, permissions: readonly Permission[]) {
  return {
    ...view,
    allowedActions: view.allowedActions.filter((action) =>
      permissions.includes(ACTION_PERMISSION[action]),
    ),
  };
}

/** Liste satırı her rol için aynıdır; beklenen yük listede yoktur. */
export function toSummary(shipment: InternalShipmentView): ShipmentSummary {
  return {
    ...toBase(shipment),
    subcontractorId: shipment.subcontractorId,
    ...(shipment.driverName === undefined ? {} : { driverName: shipment.driverName }),
  };
}

/** Tüm kullanıcılar fabrika personelidir (KR-17); site kapsamı mock'ta tektir. */
export function isInScope(_shipment: InternalShipmentView, user: CurrentUser): boolean {
  return user.siteIds.length > 0;
}
