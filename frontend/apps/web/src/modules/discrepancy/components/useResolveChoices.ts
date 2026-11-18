import { usePermission } from '@/modules/auth';
import { RESOLUTIONS } from './resolveChoices';

/** Seçenekler herkes için aynıdır; yalnızca müdürün ekran metinleri (düz dil) ayrıdır. */
export function useResolveChoices() {
  const isManager = !usePermission('DISCREPANCY_RESOLVE');
  return { isManager, choices: RESOLUTIONS };
}
