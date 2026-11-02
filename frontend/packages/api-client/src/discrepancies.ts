export {
  assignDiscrepancy,
  DiscrepancyStatus,
  getDiscrepancy,
  getDiscrepancySettings,
  listDiscrepancies,
  listDiscrepancyActivity,
  resolveDiscrepancy,
  Resolution,
  Severity,
  updateDiscrepancySettings,
} from './generated/discrepancyService';
export type {
  DiscrepancyActivityView,
  DiscrepancyPage,
  DiscrepancySettingsView,
  DiscrepancyView,
  ListDiscrepanciesParams,
  ResolveDiscrepancyRequest,
  TimeLimitVersionView,
  ToleranceVersionView,
  UpdateDiscrepancySettingsRequest,
} from './generated/discrepancyService';

export const DISCREPANCIES_PATH = '/api/v1/discrepancies';
export const DISCREPANCY_SETTINGS_PATH = '/api/v1/discrepancy-settings';
