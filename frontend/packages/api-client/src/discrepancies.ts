export {
  assignDiscrepancy,
  DiscrepancyStatus,
  getDiscrepancy,
  getShipmentClaims,
  listDiscrepancies,
  listDiscrepancyActivity,
  openQualityClaim,
  resolveDiscrepancy,
  Resolution,
  Severity,
} from './generated/discrepancyService';
export type {
  DiscrepancyActivityView,
  DiscrepancyPage,
  DiscrepancyView,
  ListDiscrepanciesParams,
  OpenQualityClaimRequest,
  QualityClaimView,
  ResolveDiscrepancyRequest,
  ShipmentClaimsView,
} from './generated/discrepancyService';

export const DISCREPANCIES_PATH = '/api/v1/discrepancies';
export const QUALITY_CLAIMS_PATH = '/api/v1/quality-claims';
