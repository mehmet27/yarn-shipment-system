import type { BalanceMatrixCell } from '@iplik/api-client';

export type MatrixColumn = { id: string; name: string };

export type BalanceMatrix = {
  columns: MatrixColumn[];
  rows: {
    yarnType: string;
    /** fasoncu kimliği → bakiye kg */
    values: Record<string, number>;
    totalKg: number;
    inTransitKg: number;
  }[];
  columnTotals: Record<string, number>;
  grandTotalKg: number;
  grandInTransitKg: number;
};

const FACTOR = 1000;
const round3 = (value: number): number => Math.round(value * FACTOR) / FACTOR;

/** Satır = iplik cinsi, sütun = fasoncu; toplam satırı/sütunu ve ayrı "yolda" sütunu. */
export function pivotBalanceMatrix(cells: BalanceMatrixCell[]): BalanceMatrix {
  const columnMap = new Map<string, string>();
  const rowMap = new Map<string, { values: Record<string, number>; inTransit: number }>();
  for (const cell of cells) {
    columnMap.set(cell.subcontractorId, cell.subcontractorName);
    const row = rowMap.get(cell.yarnType) ?? { values: {}, inTransit: 0 };
    row.values[cell.subcontractorId] =
      (row.values[cell.subcontractorId] ?? 0) + Number(cell.balanceKg);
    row.inTransit += Number(cell.inTransitKg);
    rowMap.set(cell.yarnType, row);
  }
  const columns = [...columnMap].map(([id, name]) => ({ id, name }));
  columns.sort((a, b) => a.name.localeCompare(b.name, 'tr'));
  const columnTotals: Record<string, number> = {};
  for (const column of columns) columnTotals[column.id] = 0;
  const rows = [...rowMap]
    .sort(([a], [b]) => a.localeCompare(b, 'tr'))
    .map(([yarnType, { values, inTransit }]) => {
      let total = 0;
      for (const column of columns) {
        const value = round3(values[column.id] ?? 0);
        values[column.id] = value;
        total += value;
        columnTotals[column.id] = round3((columnTotals[column.id] ?? 0) + value);
      }
      return { yarnType, values, totalKg: round3(total), inTransitKg: round3(inTransit) };
    });
  return {
    columns,
    rows,
    columnTotals,
    grandTotalKg: round3(rows.reduce((sum, row) => sum + row.totalKg, 0)),
    grandInTransitKg: round3(rows.reduce((sum, row) => sum + row.inTransitKg, 0)),
  };
}
