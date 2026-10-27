export {
  assignDiscrepancy,
  DiscrepancyStatus,
  getDiscrepancy,
  listDiscrepancies,
  listDiscrepancyActivity,
  resolveDiscrepancy,
  Resolution,
  Severity,
} from './generated/discrepancyService';
export type {
  DiscrepancyActivityView,
  DiscrepancyPage,
  DiscrepancyView,
  ListDiscrepanciesParams,
  ResolveDiscrepancyRequest,
} from './generated/discrepancyService';

export const DISCREPANCIES_PATH = '/api/v1/discrepancies';
