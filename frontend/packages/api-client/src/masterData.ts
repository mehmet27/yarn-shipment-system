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

export {
  activateDriver,
  activateSite,
  activateSubcontractor,
  activateVehicle,
  createDriver,
  createSite,
  createSubcontractor,
  createVehicle,
  deactivateDriver,
  deactivateSite,
  deactivateSubcontractor,
  deactivateVehicle,
  updateDriver,
  updateSite,
  updateSubcontractor,
  updateVehicle,
} from './generated/shipmentService';
export type {
  DriverRequest,
  DriverView,
  SiteRequest,
  SiteView,
  SubcontractorRequest,
  SubcontractorView,
  VehicleRequest,
  VehicleView,
} from './generated/shipmentService';

export { assignShipmentBags } from './generated/shipmentService';
export type {
  AssignShipmentBagsRequest,
  AssignShipmentBagsResponse,
} from './generated/shipmentService';
