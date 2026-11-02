import type { LotView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Badge } from '@iplik/ui';

const DATE_TIME_FORMATTER = new Intl.DateTimeFormat('tr-TR', {
  dateStyle: 'short',
  timeStyle: 'short',
});

/** Kilitsiz ya da kilit anıyla kilitli rozeti. */
export function LotLockState({ lot }: { lot: LotView }) {
  const { t } = useTranslation();
  if (lot.qualityLockedAt === undefined) {
    return <Badge color="orange">{t('lots.unlocked')}</Badge>;
  }
  return (
    <Badge color="green">
      {t('lots.lockedAt', { at: DATE_TIME_FORMATTER.format(new Date(lot.qualityLockedAt)) })}
    </Badge>
  );
}
