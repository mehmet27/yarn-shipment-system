import { usePermission } from '@/modules/auth';
import { useIsInvoiceable } from '../hooks/useDecideDiscrepancy';
import { choicesFor } from './resolveChoices';

/** Kararı yalnızca DISCREPANCY_DECIDE'li (müdür) kişi için kısıtlı seçenekler; kalite hepsini görür. */
export function useResolveChoices(shipmentId: string) {
  const isManager = !usePermission('DISCREPANCY_RESOLVE');
  const canInvoice = usePermission('INVOICE_MARK');
  const isInvoiceable = useIsInvoiceable(shipmentId, isManager && canInvoice);
  return { isManager, choices: choicesFor(isManager, isInvoiceable) };
}
