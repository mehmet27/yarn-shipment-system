import type { Resolution } from '@iplik/api-client';

/** Kalite her kararı verir; müdür yalnızca haklı/haksız der. Haklı kararın faturası Faturalanacaklar'dan girilir. */
export function choicesFor(isManager: boolean): Resolution[] {
  return isManager
    ? ['CLAIM_JUSTIFIED', 'CLAIM_UNJUSTIFIED']
    : ['CLAIM_JUSTIFIED', 'CLAIM_UNJUSTIFIED', 'LIABILITY_ASSIGNED'];
}
