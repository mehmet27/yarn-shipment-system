import { todayIsoDate } from '@iplik/i18n';

/** Rapor dönemi seçenekleri: son N gün (bugün dahil). */
export const PERIOD_DAYS = ['7', '30', '90'] as const;
export type PeriodDays = (typeof PERIOD_DAYS)[number];
export const DEFAULT_PERIOD: PeriodDays = '30';

const MS_PER_DAY = 86_400_000;

/**
 * Son `days` günün sınırları, ISO tarih olarak. Sunucu uçları İstanbul takvim günüdür; bu yüzden günler UTC'ye değil
 * İstanbul saatine göre hesaplanır (gece 00:00-03:00 arası UTC tarihi dünü gösterir ve bugünün kayıtları dışarıda
 * kalırdı). `today` testlerde sabitlenir.
 */
export function toPeriodParams(
  days: PeriodDays,
  today: Date = new Date(),
): { from: string; to: string } {
  const from = new Date(today.getTime() - (Number(days) - 1) * MS_PER_DAY);
  return { from: todayIsoDate(from), to: todayIsoDate(today) };
}
