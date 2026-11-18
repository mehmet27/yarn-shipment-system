import { usePermission } from '@/modules/auth';
import { choicesFor } from './resolveChoices';

/** Kararı yalnızca DISCREPANCY_DECIDE'li (müdür) kişi için kısıtlı seçenekler; kalite hepsini görür. */
export function useResolveChoices() {
  const isManager = !usePermission('DISCREPANCY_RESOLVE');
  return { isManager, choices: choicesFor(isManager) };
}
