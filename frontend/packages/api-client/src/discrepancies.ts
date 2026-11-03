export {
  assignDiscrepancy,
  DiscrepancyStatus,
  getDiscrepancy,
  getDiscrepancySettings,
  getShipmentClaims,
  InspectionKind,
  listDiscrepancies,
  listDiscrepancyActivity,
  listDiscrepancyTickets,
  markTicketMisread,
  openQualityClaim,
  recordInspectionResult,
  requestInspection,
  resolveDiscrepancy,
  Resolution,
  Severity,
  updateDiscrepancySettings,
} from './generated/discrepancyService';
export type {
  DiscrepancyActivityView,
  DiscrepancyPage,
  DiscrepancySettingsView,
  DiscrepancyTicketView,
  TicketMisreadView,
  DiscrepancyView,
  InspectionView,
  ListDiscrepanciesParams,
  MarkTicketMisreadRequest,
  OpenQualityClaimRequest,
  QualityClaimView,
  RecordInspectionResultRequest,
  RequestInspectionRequest,
  ResolveDiscrepancyRequest,
  ShipmentClaimsView,
  TimeLimitVersionView,
  ToleranceVersionView,
  UpdateDiscrepancySettingsRequest,
} from './generated/discrepancyService';

export const DISCREPANCIES_PATH = '/api/v1/discrepancies';
export const DISCREPANCY_SETTINGS_PATH = '/api/v1/discrepancy-settings';
export const QUALITY_CLAIMS_PATH = '/api/v1/quality-claims';
