import type {
  DashboardView,
  PartyStatsView,
  ShipmentRowView,
  SubcontractorBalanceView,
} from '@iplik/api-client';
import { describe, expect, it } from 'vitest';
import { buildTables } from './buildTables';
import { formatCell } from './formatCell';
import type { ReportBundle, ReportCell, ReportMeta, ReportTable } from './reportModel';
import { filterRows, partyTotals } from './reportModel';
import {
  buildWorkbookBlob,
  columnWidths,
  reportFileName,
  sheetName,
  toSheetData,
} from './writeWorkbook';

const dashboard: DashboardView = {
  from: '2026-10-05',
  to: '2026-11-03',
  shipments: { total: 12, inProgress: 4, inReview: 2, completed: 5, cancelled: 1 },
  quantity: {
    dispatchNetKg: '24000.000',
    deliveryNetKg: '23820.500',
  },
  discrepancies: { open: 3, red: 1, yellow: 2, overdue: 1 },
  overdueDiscrepancies: [
    {
      discrepancyId: 'd1',
      discrepancyNo: 'UY-1',
      shipmentId: 's1',
      severity: 'RED',
      dueAt: '2026-11-01T09:00:00Z',
    },
  ],
};
const subcontractors: PartyStatsView[] = [
  {
    id: 'aaaaaaaa-1',
    shipmentCount: 4,
    discrepantShipmentCount: 2,
    discrepancyRate: '50.00',
  },
  {
    id: 'bbbbbbbb-2',
    shipmentCount: 6,
    discrepantShipmentCount: 0,
    discrepancyRate: '0.00',
  },
];
const shipments: ShipmentRowView[] = [
  {
    shipmentId: 's1',
    shipmentNo: 'SV-1',
    subcontractorId: 'cccccccc-3',
    status: 'COMPLETED',
    dispatchNetKg: '1000.000',
    deliveryNetKg: '900.000',
    hadDiscrepancy: true,
    createdAt: '2026-11-02T09:00:00Z',
  },
  {
    shipmentId: 's2',
    shipmentNo: 'SV-2',
    subcontractorId: 'cccccccc-3',
    status: 'AWAITING_DELIVERY',
    dispatchNetKg: '500.000',
    hadDiscrepancy: false,
    createdAt: '2026-11-03T09:00:00Z',
  },
];
const balances: SubcontractorBalanceView[] = [
  {
    subcontractorId: 'cccccccc-3',
    shipmentCount: 2,
    dispatchedKg: '1500.000',
    deliveredKg: '900.000',
    inTransitKg: '500.000',
  },
];
const bundle: ReportBundle = {
  from: dashboard.from,
  to: dashboard.to,
  dashboard,
  subcontractors,
  shipments,
  balances,
};
const meta: ReportMeta = {
  from: dashboard.from,
  to: dashboard.to,
  generatedAt: new Date('2026-11-04T08:00:00Z'),
  generatedBy: 'Ayşe Yılmaz',
  onlyDiscrepant: false,
  nameOf: (id) => (id.startsWith('aaaaaaaa') ? 'Ali Veli' : id.slice(0, 8)),
};
const t = (key: string) => key;

describe('rapor modeli', () => {
  it('toplam satırı oranı toplam sayılardan hesaplar', () => {
    const totals = partyTotals(subcontractors);
    expect(totals.shipments).toBe(10);
    expect(totals.discrepant).toBe(2);
    expect(totals.rate).toBe(20);
  });

  it('boş listede toplam sıfırdır (sıfıra bölme yok)', () => {
    expect(partyTotals([])).toEqual({ shipments: 0, discrepant: 0, rate: 0 });
  });

  it('uyuşmazlık süzgeci yalnızca uyuşmazlıklı satırları bırakır', () => {
    expect(filterRows(subcontractors, true)).toHaveLength(1);
    expect(filterRows(subcontractors, false)).toHaveLength(2);
  });
});

describe('tablolar ve biçim', () => {
  it('beş tablo üretir: özet, süresi geçenler, fasoncu, döküm, bakiye', () => {
    const tables = buildTables(bundle, meta, t);
    expect(tables.map((table) => table.title)).toEqual([
      'report.export.sheets.summary',
      'report.export.sheets.overdue',
      'report.export.sheets.subcontractors',
      'report.export.sheets.shipments',
      'report.export.sheets.balance',
    ]);
  });

  it('süzgeç tablolara da uygulanır; toplam yalnızca görünen satırlardan hesaplanır', () => {
    const [, , partyTable] = buildTables(bundle, { ...meta, onlyDiscrepant: true }, t);
    expect(partyTable?.rows).toHaveLength(1);
    expect(formatCell(partyTable?.total?.[1] ?? { kind: 'text', value: '' })).toBe('4');
  });

  it('boş tabloda toplam satırı yoktur', () => {
    const empty = { ...bundle, subcontractors: [] };
    const [, , partyTable] = buildTables(empty, meta, t);
    expect(partyTable?.total).toBeUndefined();
  });

  it('metin hücreleri Türkçe biçimlenir', () => {
    expect(formatCell({ kind: 'kg', value: 1234.5 })).toBe('1.234,500 kg');
    expect(formatCell({ kind: 'percent', value: 0.5 })).toBe('%50,00');
    expect(formatCell({ kind: 'count', value: 3 })).toBe('3');
  });
});

describe('adlar', () => {
  it('çözülen kimlik ad olarak, çözülemeyen kısaltma olarak yazılır', () => {
    const [, , partyTable] = buildTables(bundle, meta, t);
    expect(partyTable?.rows[0]?.[0]).toEqual({ kind: 'text', value: 'Ali Veli' });
    expect(partyTable?.rows[1]?.[0]).toEqual({ kind: 'text', value: 'bbbbbbbb' });
  });
});

function cellAt(
  table: ReportTable | undefined,
  row: number,
  column: number,
): ReportCell | undefined {
  return table?.rows[row]?.[column];
}

function totalAt(table: ReportTable | undefined, column: number): ReportCell | undefined {
  return table?.total?.[column];
}

describe('sevkiyat dökümü ve bakiye', () => {
  const emptyText = { kind: 'text', value: '' };

  it('dökümde eksik alanlar boş kalır', () => {
    const table = buildTables(bundle, meta, t)[3];
    expect(table?.rows).toHaveLength(2);
    expect(cellAt(table, 0, 1)).toEqual({ kind: 'text', value: 'cccccccc' });
    expect(cellAt(table, 1, 4)).toEqual(emptyText);
    expect(cellAt(table, 1, 7)).toEqual(emptyText);
  });

  it('dökümün toplam satırı kiloları ve uyuşmazlık sayısını verir', () => {
    const table = buildTables(bundle, meta, t)[3];
    expect(totalAt(table, 3)).toEqual({ kind: 'kg', value: 1500 });
    expect(totalAt(table, 4)).toEqual({ kind: 'kg', value: 900 });
    expect(totalAt(table, 5)).toEqual({ kind: 'count', value: 1 });
  });

  it('uyuşmazlık süzgeci dökümü de daraltır; boş dökümde toplam satırı yoktur', () => {
    const filtered = buildTables(bundle, { ...meta, onlyDiscrepant: true }, t)[3];
    expect(filtered?.rows).toHaveLength(1);
    const none = buildTables({ ...bundle, shipments: [] }, meta, t)[3];
    expect(none?.total).toBeUndefined();
  });

  it('bakiye tablosu fasoncu adını verir, toplam satırı toplar', () => {
    const table = buildTables(bundle, meta, t)[4];
    expect(cellAt(table, 0, 0)).toEqual({ kind: 'text', value: 'cccccccc' });
    expect(totalAt(table, 4)).toEqual({ kind: 'kg', value: 500 });
    expect(buildTables({ ...bundle, balances: [] }, meta, t)[4]?.total).toBeUndefined();
  });
});

describe('Excel çıktısı', () => {
  it('sayısal hücreler metne çevrilmez; kg ve oran biçim kodu taşır', () => {
    const [, , partyTable] = buildTables(bundle, meta, t);
    if (partyTable === undefined) throw new Error('tablo yok');
    const data = toSheetData(partyTable);
    expect(data).toHaveLength(1 + 2 + 1);
    const row = data[1];
    expect(row?.[1]).toMatchObject({ type: Number, value: 4 });
    expect(row?.[3]).toMatchObject({ type: Number, value: 0.5, format: '0.00%' });
    expect(data[3]?.[0]).toMatchObject({ fontWeight: 'bold' });
  });

  it('sayfa adı 31 karakteri geçmez ve yasak karakter içermez', () => {
    expect(sheetName('A/B:C?'.padEnd(50, 'x'))).toHaveLength(31);
    expect(sheetName('A/B:C?')).toBe('A B C ');
  });

  it('kolon genişliği alt ve üst sınırla kısıtlıdır', () => {
    const [table] = buildTables(bundle, meta, t);
    if (table === undefined) throw new Error('tablo yok');
    for (const column of columnWidths(table)) {
      expect(column.width).toBeGreaterThanOrEqual(12);
      expect(column.width).toBeLessThanOrEqual(48);
    }
  });

  it('geçerli bir xlsx (zip) dosyası üretir', async () => {
    const blob = await buildWorkbookBlob(buildTables(bundle, meta, t));
    const bytes = new Uint8Array(await blob.arrayBuffer());
    expect(String.fromCharCode(bytes[0] ?? 0, bytes[1] ?? 0)).toBe('PK');
    expect(blob.size).toBeGreaterThan(1000);
  });

  it('dosya adı dönemi içerir', () => {
    expect(reportFileName('2026-10-05', '2026-11-03', 'xlsx')).toBe(
      'rapor_2026-10-05_2026-11-03.xlsx',
    );
  });
});
