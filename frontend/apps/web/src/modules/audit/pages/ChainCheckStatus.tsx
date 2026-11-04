import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Alert } from '@iplik/ui';
import { useLatestChainCheck } from '../hooks/useAuditLog';

/** Günlük zincir doğrulamasının son sonucu; bozulma varsa kırmızı uyarı verir. */
export function ChainCheckStatus() {
  const { t } = useTranslation();
  const query = useLatestChainCheck();
  const latest = query.data?.[0];
  if (latest === undefined) return null;
  const checkedAt = formatDateTime(latest.checkedAt);
  return latest.ok ? (
    <Alert color="green">{t('audit.chain.ok', { at: checkedAt })}</Alert>
  ) : (
    <Alert color="red">
      {t('audit.chain.broken', { at: checkedAt, id: latest.brokenAtId ?? latest.fromId })}
    </Alert>
  );
}
