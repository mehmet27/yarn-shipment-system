import type {
  DashboardView,
  PartyStatsView,
  ShipmentRowView,
  SubcontractorBalanceView,
} from '@iplik/api-client';

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
  subcontractors: PartyStatsView[];
  shipments: ShipmentRowView[];
  balances: SubcontractorBalanceView[];
};

export type ReportMeta = {
  from: string;
  to: string;
  generatedAt: Date;
  generatedBy: string;
  onlyDiscrepant: boolean;
  /** Fasoncu kimliğini ada çevirir; bilinmiyorsa kısaltma döner */
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
};

/** "Yalnızca uyuşmazlıklılar" süzgeci; tüm rapor türlerinde aynı kural. */
export function filterRows<T extends PartyLike>(rows: T[], onlyDiscrepant: boolean): T[] {
  return onlyDiscrepant ? rows.filter((row) => row.discrepantShipmentCount > 0) : rows;
}

/** Toplam satırı: oran toplam sayılardan hesaplanır, satır oranlarının ortalaması değildir. */
export function partyTotals(rows: PartyLike[]): {
  shipments: number;
  discrepant: number;
  rate: number;
} {
  const shipments = rows.reduce((sum, row) => sum + row.shipmentCount, 0);
  const discrepant = rows.reduce((sum, row) => sum + row.discrepantShipmentCount, 0);
  return {
    shipments,
    discrepant,
    rate: shipments === 0 ? 0 : (discrepant / shipments) * PERCENT_SCALE,
  };
}

/** Boş olabilen kilo/yüzde dizesi → hücre; yoksa boş metin. */
export const optionalKg = (value: string | undefined): ReportCell =>
  value === undefined ? text('') : kg(value);

export function sumKg(values: (string | undefined)[]): number {
  return round3(values.reduce((sum, value) => sum + (value === undefined ? 0 : Number(value)), 0));
}
