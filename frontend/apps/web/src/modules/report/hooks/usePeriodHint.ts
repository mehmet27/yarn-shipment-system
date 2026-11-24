import { useTranslation } from '@iplik/i18n';
import { toChange } from '../comparison';

/**
 * Kartın altındaki "önceki dönem" satırını üretir: önceki dönemin değeri ve değişim yüzdesi. Önceki dönem henüz
 * gelmediyse satır yazılmaz.
 */
export function usePeriodHint() {
  const { t } = useTranslation();
  return (current: number, previous: number | undefined, previousText: string) => {
    if (previous === undefined) return undefined;
    const change = toChange(current, previous);
    const base = t('report.compare.previous', { value: previousText });
    if (change === null) return base;
    return `${base} · ${t(`report.compare.${change.direction}`, { percent: change.percent })}`;
  };
}
