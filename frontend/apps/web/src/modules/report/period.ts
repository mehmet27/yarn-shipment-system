/** Rapor dönemi seçenekleri: son N gün (bugün dahil). */
export const PERIOD_DAYS = ['7', '30', '90'] as const;
export type PeriodDays = (typeof PERIOD_DAYS)[number];
export const DEFAULT_PERIOD: PeriodDays = '30';

const MS_PER_DAY = 86_400_000;
const ISO_DATE_LENGTH = 10;

function toIsoDate(date: Date): string {
  return date.toISOString().slice(0, ISO_DATE_LENGTH);
}

/** Son `days` günün sınırları, ISO tarih olarak. `today` testlerde sabitlenir. */
export function toPeriodParams(
  days: PeriodDays,
  today: Date = new Date(),
): { from: string; to: string } {
  const from = new Date(today.getTime() - (Number(days) - 1) * MS_PER_DAY);
  return { from: toIsoDate(from), to: toIsoDate(today) };
}
