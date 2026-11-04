import type {
  DashboardView,
  PairStatsView,
  PartyStatsView,
  ShipmentRowView,
  SubcontractorBalanceView,
} from '@iplik/api-client';
import { describe, expect, it } from 'vitest';
import { buildTables } from './buildTables';
import { formatCell } from './formatCell';
import type { ReportBundle, ReportCell, ReportMeta, ReportTable } from './reportModel';
import { differencePercent, filterRows, partyTotals } from './reportModel';
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
    absoluteDiffKg: '240.000',
    comparedShipments: 7,
  },
  bands: { green: 4, yellow: 2, red: 1 },
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
  largestDifferences: [
    { shipmentId: 's1', shipmentNo: 'SV-1', diffKg: '180.000', diffPct: '3.60', band: 'RED' },
  ],
};
const drivers: PartyStatsView[] = [
  {
    id: 'aaaaaaaa-1',
    shipmentCount: 4,
    discrepantShipmentCount: 2,
    discrepancyRate: '50.00',
    averageAbsoluteDiffKg: '60.000',
  },
  {
    id: 'bbbbbbbb-2',
    shipmentCount: 6,
    discrepantShipmentCount: 0,
    discrepancyRate: '0.00',
    averageAbsoluteDiffKg: '10.000',
  },
];
const pairs: PairStatsView[] = [
  {
    driverId: 'aaaaaaaa-1',
    subcontractorId: 'cccccccc-3',
    shipmentCount: 4,
    discrepantShipmentCount: 2,
    discrepancyCount: 3,
    discrepancyRate: '50.00',
    averageAbsoluteDiffKg: '60.000',
  },
];
const shipments: ShipmentRowView[] = [
  {
    shipmentId: 's1',
    shipmentNo: 'SV-1',
    subcontractorId: 'cccccccc-3',
    driverId: 'aaaaaaaa-1',
    status: 'IN_REVIEW',
    dispatchNetKg: '1000.000',
    deliveryNetKg: '900.000',
    diffKg: '100.000',
    diffPct: '10.00',
    band: 'RED',
    hadDiscrepancy: true,
    createdAt: '2026-11-02T09:00:00Z',
  },
  {
    shipmentId: 's2',
    shipmentNo: 'SV-2',
    subcontractorId: 'cccccccc-3',
    status: 'IN_TRANSIT',
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
    differenceKg: '100.000',
  },
];
const bundle: ReportBundle = {
  from: dashboard.from,
  to: dashboard.to,
  dashboard,
  drivers,
  subcontractors: drivers,
  pairs,
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
  it('toplam satırı oranı ve ortalamayı sevkiyat sayısıyla ağırlıklandırır', () => {
    const totals = partyTotals(drivers);
    expect(totals.shipments).toBe(10);
    expect(totals.discrepant).toBe(2);
    expect(totals.rate).toBe(20);
    expect(totals.averageDiffKg).toBe(30);
  });

  it('boş listede toplam sıfırdır (sıfıra bölme yok)', () => {
    expect(partyTotals([])).toEqual({ shipments: 0, discrepant: 0, rate: 0, averageDiffKg: 0 });
  });

  it('uyuşmazlık süzgeci yalnızca uyuşmazlıklı satırları bırakır', () => {
    expect(filterRows(drivers, true)).toHaveLength(1);
    expect(filterRows(drivers, false)).toHaveLength(2);
  });

  it('mutlak fark oranı çıkış net üzerinden hesaplanır; çıkış 0 ise 0', () => {
    expect(differencePercent(dashboard)).toBeCloseTo(1, 5);
    expect(
      differencePercent({
        ...dashboard,
        quantity: { ...dashboard.quantity, dispatchNetKg: '0.000' },
      }),
    ).toBe(0);
  });
});

describe('tablolar ve biçim', () => {
  it('sekiz tablo üretir: özet, süresi geçenler, en büyük farklar, şoför, fasoncu, eşleşme, döküm, bakiye', () => {
    const tables = buildTables(bundle, meta, t);
    expect(tables.map((table) => table.title)).toEqual([
      'report.export.sheets.summary',
      'report.export.sheets.overdue',
      'report.export.sheets.largest',
      'report.export.sheets.drivers',
      'report.export.sheets.subcontractors',
      'report.export.sheets.pairs',
      'report.export.sheets.shipments',
      'report.export.sheets.balance',
    ]);
  });

  it('süzgeç tablolara da uygulanır; toplam yalnızca görünen satırlardan hesaplanır', () => {
    const [, , , driverTable] = buildTables(bundle, { ...meta, onlyDiscrepant: true }, t);
    expect(driverTable?.rows).toHaveLength(1);
    expect(formatCell(driverTable?.total?.[1] ?? { kind: 'text', value: '' })).toBe('4');
  });

  it('boş tabloda toplam satırı yoktur', () => {
    const empty = { ...bundle, drivers: [] };
    const [, , , driverTable] = buildTables(empty, meta, t);
    expect(driverTable?.total).toBeUndefined();
  });

  it('metin hücreleri Türkçe biçimlenir', () => {
    expect(formatCell({ kind: 'kg', value: 1234.5 })).toBe('1.234,500 kg');
    expect(formatCell({ kind: 'percent', value: 0.5 })).toBe('%50,00');
    expect(formatCell({ kind: 'count', value: 3 })).toBe('3');
  });
});

describe('adlar', () => {
  it('çözülen kimlik ad olarak, çözülemeyen kısaltma olarak yazılır', () => {
    const [, , , driverTable, , pairTable] = buildTables(bundle, meta, t);
    expect(driverTable?.rows[0]?.[0]).toEqual({ kind: 'text', value: 'Ali Veli' });
    expect(driverTable?.rows[1]?.[0]).toEqual({ kind: 'text', value: 'bbbbbbbb' });
    expect(pairTable?.rows[0]?.slice(0, 2)).toEqual([
      { kind: 'text', value: 'Ali Veli' },
      { kind: 'text', value: 'cccccccc' },
    ]);
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
    const table = buildTables(bundle, meta, t)[6];
    expect(table?.rows).toHaveLength(2);
    expect(cellAt(table, 0, 2)).toEqual({ kind: 'text', value: 'Ali Veli' });
    expect(cellAt(table, 1, 2)).toEqual(emptyText);
    expect(cellAt(table, 1, 5)).toEqual(emptyText);
  });

  it('dökümün toplam satırı kiloları ve uyuşmazlık sayısını verir', () => {
    const table = buildTables(bundle, meta, t)[6];
    expect(totalAt(table, 4)).toEqual({ kind: 'kg', value: 1500 });
    expect(totalAt(table, 5)).toEqual({ kind: 'kg', value: 900 });
    expect(totalAt(table, 9)).toEqual({ kind: 'count', value: 1 });
  });

  it('uyuşmazlık süzgeci dökümü de daraltır; boş dökümde toplam satırı yoktur', () => {
    const filtered = buildTables(bundle, { ...meta, onlyDiscrepant: true }, t)[6];
    expect(filtered?.rows).toHaveLength(1);
    const none = buildTables({ ...bundle, shipments: [] }, meta, t)[6];
    expect(none?.total).toBeUndefined();
  });

  it('bakiye tablosu fasoncu adını ve fireyi verir, toplam satırı toplar', () => {
    const table = buildTables(bundle, meta, t)[7];
    expect(cellAt(table, 0, 0)).toEqual({ kind: 'text', value: 'cccccccc' });
    expect(cellAt(table, 0, 5)).toEqual({ kind: 'kg', value: 100 });
    expect(totalAt(table, 4)).toEqual({ kind: 'kg', value: 500 });
    expect(buildTables({ ...bundle, balances: [] }, meta, t)[7]?.total).toBeUndefined();
  });
});

describe('Excel çıktısı', () => {
  it('sayısal hücreler metne çevrilmez; kg ve oran biçim kodu taşır', () => {
    const [, , , driverTable] = buildTables(bundle, meta, t);
    if (driverTable === undefined) throw new Error('tablo yok');
    const data = toSheetData(driverTable);
    expect(data).toHaveLength(1 + 2 + 1);
    const row = data[1];
    expect(row?.[1]).toMatchObject({ type: Number, value: 4 });
    expect(row?.[3]).toMatchObject({ type: Number, value: 0.5, format: '0.00%' });
    expect(row?.[4]).toMatchObject({ type: Number, value: 60, format: '#,##0.000' });
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
