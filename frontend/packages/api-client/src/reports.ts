export {
  getDashboard,
  getDriverReport,
  getPairReport,
  getShipmentReport,
  getSubcontractorBalanceReport,
  getSubcontractorReport,
} from './generated/reportingService';
export type {
  DashboardView,
  GetDashboardParams,
  LargestDifferenceView,
  OpenDiscrepanciesView,
  OverdueDiscrepancyView,
  PairStatsView,
  PartyStatsView,
  ShipmentRowView,
  SubcontractorBalanceView,
} from './generated/reportingService';

export const REPORTS_PATH = '/api/v1/reports';
