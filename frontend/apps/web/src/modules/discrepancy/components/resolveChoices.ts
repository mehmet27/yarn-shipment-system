import type { Resolution } from '@iplik/api-client';

/** Müdürün "Haklı, faturalandı" seçimi; karar haklıdır ve fatura aynı adımda girilir. */
export const JUSTIFIED_INVOICED = 'CLAIM_JUSTIFIED_INVOICED';

export type ResolveChoice = Resolution | typeof JUSTIFIED_INVOICED;

/** Seçimin sunucuya giden kararı. */
export const resolutionOf = (choice: ResolveChoice): Resolution =>
  choice === JUSTIFIED_INVOICED ? 'CLAIM_JUSTIFIED' : choice;

/** Kalite her kararı verir; müdür yalnızca haklı/haksız der, fatura bekleyen dosyada "haklı, faturalandı" da çıkar. */
export function choicesFor(isManager: boolean, isInvoiceable: boolean): ResolveChoice[] {
  if (!isManager) return ['CLAIM_JUSTIFIED', 'CLAIM_UNJUSTIFIED', 'LIABILITY_ASSIGNED'];
  return isInvoiceable
    ? [JUSTIFIED_INVOICED, 'CLAIM_JUSTIFIED', 'CLAIM_UNJUSTIFIED']
    : ['CLAIM_JUSTIFIED', 'CLAIM_UNJUSTIFIED'];
}
