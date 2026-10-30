export {
  assignDiscrepancy,
  DiscrepancyStatus,
  getDiscrepancy,
  getShipmentClaims,
  InspectionKind,
  listDiscrepancies,
  listDiscrepancyActivity,
  openQualityClaim,
  recordInspectionResult,
  requestInspection,
  resolveDiscrepancy,
  Resolution,
  Severity,
} from './generated/discrepancyService';
export type {
  DiscrepancyActivityView,
  DiscrepancyPage,
  DiscrepancyView,
  InspectionView,
  ListDiscrepanciesParams,
  OpenQualityClaimRequest,
  QualityClaimView,
  RecordInspectionResultRequest,
  RequestInspectionRequest,
  ResolveDiscrepancyRequest,
  ShipmentClaimsView,
} from './generated/discrepancyService';

export const DISCREPANCIES_PATH = '/api/v1/discrepancies';
export const QUALITY_CLAIMS_PATH = '/api/v1/quality-claims';
