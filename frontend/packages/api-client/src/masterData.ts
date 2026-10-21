export {
  createShipment,
  listDrivers,
  listSites,
  listSubcontractors,
  listVehicles,
} from './generated/shipmentService';
export type {
  CreateShipmentRequest,
  CreateShipmentResponse,
  ListDriversParams,
} from './generated/shipmentService';

export { getShipmentBags, getShipmentHistory } from './generated/shipmentService';
export type {
  DeliveryScanView,
  DispatchBagView,
  ShipmentBagsResponse,
  ShipmentHistoryResponse,
  StatusChangeView,
} from './generated/shipmentService';
