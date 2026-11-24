import { todayIsoDate } from '@iplik/i18n';
import type { Period } from './comparison';

const MS_PER_DAY = 86_400_000;
const MONTH_LENGTH = 'YYYY-MM'.length;
const DAY_LENGTH = 'YYYY-MM-DD'.length;
const MONTHS_PER_YEAR = 12;
/** Sunucunun izin verdiği en uzun rapor dönemi 366 gündür; faturalanacaklar için ay sonundan geriye bir yıl bakılır. */
const LOOKBACK_DAYS = 365;

const monthLabel = new Intl.DateTimeFormat('tr-TR', {
  month: 'long',
  year: 'numeric',
  timeZone: 'UTC',
});

const toIso = (ms: number) => new Date(ms).toISOString().slice(0, DAY_LENGTH);

/** Bu ay ve önceki aylar, yeniden eskiye (`2026-11`, `2026-10`, ...); ay İstanbul takvimine göredir. */
export function recentMonths(today: Date = new Date(), count = MONTHS_PER_YEAR): string[] {
  const current = todayIsoDate(today).slice(0, MONTH_LENGTH);
  const year = Number(current.slice(0, 'YYYY'.length));
  const month = Number(current.slice('YYYY-'.length));
  return Array.from({ length: count }, (_, index) =>
    new Date(Date.UTC(year, month - 1 - index, 1)).toISOString().slice(0, MONTH_LENGTH),
  );
}

/** Ayın ilk ve son günü. */
export function monthPeriod(month: string): Period {
  const first = Date.parse(`${month}-01T00:00:00Z`);
  const start = new Date(first);
  const next = Date.UTC(start.getUTCFullYear(), start.getUTCMonth() + 1, 1);
  return { from: toIso(first), to: toIso(next - MS_PER_DAY) };
}

/** Ay sonunda faturası hâlâ bekleyen dosyalar için bakılan dönem: ay sonundan geriye bir yıl. */
export function toInvoicePeriod(month: string): Period {
  const { to } = monthPeriod(month);
  return { from: toIso(Date.parse(`${to}T00:00:00Z`) - LOOKBACK_DAYS * MS_PER_DAY), to };
}

/** `2026-10` → `Ekim 2026`. */
export function monthName(month: string): string {
  return monthLabel.format(new Date(`${month}-01T00:00:00Z`));
}
