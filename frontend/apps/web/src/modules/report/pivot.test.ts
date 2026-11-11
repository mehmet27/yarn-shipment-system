import { describe, expect, it } from 'vitest';
import { formatFireRate, formatKg3 } from './format';
import { parseNetKg, parseOpeningLines } from './openingImport';
import { pivotBalanceMatrix } from './pivot';
import { balanceMatrixTable, fireTable, yarnVsFabricTable } from './export/faconTables';
import { formatCell } from './export/formatCell';

type CellInit = {
  yarnType: string;
  id: string;
  name: string;
  balanceKg: string;
  inTransitKg?: string;
};
const cell = ({ yarnType, id, name, balanceKg, inTransitKg = '0' }: CellInit) => ({
  yarnType,
  subcontractorId: id,
  subcontractorName: name,
  balanceKg,
  inTransitKg,
});
const t = (key: string) => key;

describe('pivotBalanceMatrix', () => {
  const matrix = pivotBalanceMatrix([
    cell({ yarnType: 'B', id: 'a', name: 'Aksu', balanceKg: '10.500', inTransitKg: '1' }),
    cell({ yarnType: 'A', id: 'a', name: 'Aksu', balanceKg: '2.250' }),
    cell({ yarnType: 'A', id: 'k', name: 'Kırıkhan', balanceKg: '1000' }),
  ]);
  it('satır iplik, sütun fasoncu; eksik hücre 0', () => {
    expect(matrix.rows.map((r) => r.yarnType)).toEqual(['A', 'B']);
    expect(matrix.columns.map((c) => c.name)).toEqual(['Aksu', 'Kırıkhan']);
    expect(matrix.rows[1]?.values.k).toBe(0);
  });
  it('satır, sütun ve genel toplam ile yolda', () => {
    expect(matrix.rows[0]?.totalKg).toBe(1002.25);
    expect(matrix.columnTotals.a).toBe(12.75);
    expect(matrix.grandTotalKg).toBe(1012.75);
    expect(matrix.grandInTransitKg).toBe(1);
  });
  it('tablo: toplam sütunu, yolda sütunu ve toplam satırı', () => {
    const table = balanceMatrixTable(matrix, t);
    expect(table.headers.at(-1)).toBe('report.matrix.inTransit');
    expect(table.rows).toHaveLength(2);
    expect(formatCell(table.total?.at(-2) ?? { kind: 'text', value: '' })).toBe('1.012,750 kg');
  });
});

describe('biçimler', () => {
  it('kilo 3 ondalık, fire yüzde', () => {
    expect(formatKg3('1982.5')).toBe('1.982,500 kg');
    expect(formatFireRate('0.0403')).toBe('%4,03');
  });
});

describe('fire ve iplik-kumaş tabloları', () => {
  const row = {
    shipmentId: 's',
    shipmentNo: 'SV-1',
    subcontractorId: 'a',
    subcontractorName: 'Aksu',
    sentKg: '1000',
    returnedYarnKg: '0',
    fabricKg: '900',
    fireRate: '0.04',
    expectedFabricKg: '960',
    actualFireRate: '0.1',
    shortageKg: '60',
    status: 'TO_INVOICE' as const,
  };
  it('fire raporu durum sütunlu, faturalanacaklar durumsuz', () => {
    expect(fireTable([row], t, { title: 'x', withStatus: true }).headers).toHaveLength(12);
    const plain = fireTable([row], t, { title: 'x', withStatus: false });
    expect(plain.headers).toHaveLength(9);
    expect(formatCell(plain.rows[0]?.[7] ?? { kind: 'text', value: '' })).toBe('%10,00');
    expect(plain.total).toHaveLength(9);
  });
  it('boş listede toplam satırı yok', () => {
    expect(yarnVsFabricTable([], t).total).toBeUndefined();
  });
  it('iplik-kumaş toplamında fire kilolardan hesaplanır', () => {
    const table = yarnVsFabricTable(
      [
        {
          subcontractorId: 'a',
          subcontractorName: 'A',
          sentKg: '1000',
          returnedYarnKg: '0',
          fabricKg: '960',
          actualFireRate: '0.04',
          shortageKg: '0',
          overLimitShipmentCount: 0,
          overLimitFireKg: '0',
        },
      ],
      t,
    );
    expect(formatCell(table.total?.[4] ?? { kind: 'text', value: '' })).toBe('%4,00');
  });
});

describe('açılış bakiyesi ayrıştırma', () => {
  it('Türkçe ve noktalı kiloları çözer', () => {
    expect(parseNetKg('1.982,500')).toBe('1982.500');
    expect(parseNetKg('1982,5')).toBe('1982.500');
    expect(parseNetKg('12.5')).toBe('12.500');
    expect(parseNetKg('abc')).toBeUndefined();
  });
  it('satırları eşler, hataları işaretler', () => {
    const lines = parseOpeningLines('Aksu;30/1;1.000,5\nYok;30/1;5\nbozuk\nAksu;20/1;x\n\n', (n) =>
      n === 'Aksu' ? 'id-a' : undefined,
    );
    expect(lines[0]?.row?.netKg).toBe('1000.500');
    expect(lines.map((l) => l.error)).toEqual([undefined, 'unknown', 'format', 'kg']);
  });
});
