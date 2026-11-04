import type { DashboardView, PairStatsView, PartyStatsView } from '@iplik/api-client';

/** Dışa aktarma ve yazdırma için tek hücre; tür, Excel biçimini ve ekran biçimini belirler. */
export type ReportCell =
  | { kind: 'text'; value: string }
  | { kind: 'count'; value: number }
  | { kind: 'kg'; value: number }
  | { kind: 'percent'; value: number };

export type ReportTable = {
  /** Excel sayfa adı ve yazdırma başlığı */
  title: string;
  headers: string[];
  rows: ReportCell[][];
  /** Toplam satırı; kayıt yoksa verilmez */
  total?: ReportCell[];
};

export type ReportBundle = {
  from: string;
  to: string;
  dashboard: DashboardView;
  drivers: PartyStatsView[];
  subcontractors: PartyStatsView[];
  pairs: PairStatsView[];
};

export type ReportMeta = {
  from: string;
  to: string;
  generatedAt: Date;
  generatedBy: string;
  onlyDiscrepant: boolean;
  /** Şoför/fasoncu kimliğini ada çevirir; bilinmiyorsa kısaltma döner */
  nameOf: (id: string) => string;
};

export const text = (value: string): ReportCell => ({ kind: 'text', value });
export const count = (value: number): ReportCell => ({ kind: 'count', value });
export const kg = (value: string | number): ReportCell => ({ kind: 'kg', value: Number(value) });
/** Sunucu yüzdesi `"50.00"`; hücrede kesir (0,5) tutulur, Excel `%` biçimiyle gösterir. */
export const percent = (value: string | number): ReportCell => ({
  kind: 'percent',
  value: Number(value) / 100,
});

const PERCENT_SCALE = 100;
const ROUND_FACTOR = 1000;

function round3(value: number): number {
  return Math.round(value * ROUND_FACTOR) / ROUND_FACTOR;
}

type PartyLike = {
  shipmentCount: number;
  discrepantShipmentCount: number;
  discrepancyRate: string;
  averageAbsoluteDiffKg: string;
};

/** "Yalnızca uyuşmazlıklılar" süzgeci; tüm rapor türlerinde aynı kural. */
export function filterRows<T extends PartyLike>(rows: T[], onlyDiscrepant: boolean): T[] {
  return onlyDiscrepant ? rows.filter((row) => row.discrepantShipmentCount > 0) : rows;
}

/** Toplam satırı: oran ve ortalama fark sevkiyat sayısıyla ağırlıklıdır, satır oranlarının ortalaması değildir. */
export function partyTotals(rows: PartyLike[]): {
  shipments: number;
  discrepant: number;
  rate: number;
  averageDiffKg: number;
} {
  const shipments = rows.reduce((sum, row) => sum + row.shipmentCount, 0);
  const discrepant = rows.reduce((sum, row) => sum + row.discrepantShipmentCount, 0);
  const weightedDiff = rows.reduce(
    (sum, row) => sum + Number(row.averageAbsoluteDiffKg) * row.shipmentCount,
    0,
  );
  return {
    shipments,
    discrepant,
    rate: shipments === 0 ? 0 : (discrepant / shipments) * PERCENT_SCALE,
    averageDiffKg: shipments === 0 ? 0 : round3(weightedDiff / shipments),
  };
}

/** Karşılaştırılan toplam çıkış kilosuna göre mutlak fark yüzdesi; çıkış 0 ise 0. */
export function differencePercent(dashboard: DashboardView): number {
  const dispatch = Number(dashboard.quantity.dispatchNetKg);
  return dispatch === 0
    ? 0
    : (Number(dashboard.quantity.absoluteDiffKg) / dispatch) * PERCENT_SCALE;
}
