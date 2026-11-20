import type { MonthlyFlowView } from '@iplik/api-client';

export const TREND_SENT_KEY = 'sentKg';
export const TREND_FABRIC_KEY = 'fabricKg';
export const TREND_MONTH_KEY = 'month';

const monthFormat = new Intl.DateTimeFormat('tr-TR', { month: 'short', year: '2-digit' });

/** `2026-11` → `Kas 26`; eksen dar ekranda da sığsın diye kısa yazılır. */
export function formatMonth(month: string): string {
  const [year, monthNumber] = month.split('-').map(Number);
  return monthFormat.format(new Date(year ?? 0, (monthNumber ?? 1) - 1, 1));
}

export type TrendRow = { month: string; sentKg: number; fabricKg: number };

/** Sunucunun aylık satırlarını grafik satırına çevirir; sayılar yalnızca çizim içindir, tablo dizeyi biçimler. */
export function toTrendRows(flows: readonly MonthlyFlowView[]): TrendRow[] {
  return flows.map((flow) => ({
    [TREND_MONTH_KEY]: formatMonth(flow.month),
    [TREND_SENT_KEY]: Number(flow.sentKg),
    [TREND_FABRIC_KEY]: Number(flow.fabricKg),
  }));
}

/** Hiçbir ayda tamamlanan sevkiyat yoksa grafik yerine boş durum yazısı gösterilir. */
export function isTrendEmpty(flows: readonly MonthlyFlowView[]): boolean {
  return flows.every((flow) => flow.shipmentCount === 0);
}
