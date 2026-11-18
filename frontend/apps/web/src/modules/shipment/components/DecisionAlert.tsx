import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert } from '@iplik/ui';
import type { DecisionOption } from './WeightDecisionCards';

function errorKey(code: string): string {
  return `shipment.weightDecision.errors.${code === 'INVALID_SHIPMENT_STATUS' ? code : 'unknown'}`;
}

/** Karar formunun uyarısı: sunucu hatası varsa o, yoksa iptal seçiliyken geri alınamaz uyarısı. */
export function DecisionAlert({ choice, error }: { choice: DecisionOption; error: unknown }) {
  const { t } = useTranslation();
  if (error !== undefined) return <Alert color="red">{t(errorKey(toProblem(error).code))}</Alert>;
  if (choice !== 'CANCEL') return null;
  return <Alert color="orange">{t('shipment.weightDecision.cancelWarning')}</Alert>;
}
