import type { Cell, SheetData } from 'write-excel-file/browser';
import writeXlsxFile from 'write-excel-file/browser';
import { EXCEL_FORMATS, formatCell } from './formatCell';
import type { ReportCell, ReportTable } from './reportModel';

const MIN_COLUMN_WIDTH = 12;
const MAX_COLUMN_WIDTH = 48;
const WIDTH_PADDING = 2;
/** Excel sayfa adı en çok 31 karakter olabilir ve `\ / ? * [ ] :` içeremez. */
const SHEET_NAME_MAX = 31;
const SHEET_FORBIDDEN = /[\\/?*[\]:]/g;

export function sheetName(title: string): string {
  return title.replace(SHEET_FORBIDDEN, ' ').slice(0, SHEET_NAME_MAX);
}

function toExcelCell(cell: ReportCell, bold: boolean): Cell {
  const style = bold ? { fontWeight: 'bold' as const } : {};
  switch (cell.kind) {
    case 'text':
      return { type: String, value: cell.value, ...style };
    case 'count':
      return { type: Number, value: cell.value, format: EXCEL_FORMATS.count, ...style };
    case 'kg':
      return { type: Number, value: cell.value, format: EXCEL_FORMATS.kg, ...style };
    case 'percent':
      return { type: Number, value: cell.value, format: EXCEL_FORMATS.percent, ...style };
  }
}

/** Tablo → sayfa verisi: kalın başlık, sayısal hücreler (metne çevrilmez), son satırda toplam. */
export function toSheetData(table: ReportTable): SheetData {
  const header: Cell[] = table.headers.map((value) => ({
    type: String,
    value,
    fontWeight: 'bold' as const,
  }));
  const body = table.rows.map((row) => row.map((cell) => toExcelCell(cell, false)));
  const total =
    table.total === undefined ? [] : [table.total.map((cell) => toExcelCell(cell, true))];
  return [header, ...body, ...total];
}

export function columnWidths(table: ReportTable): { width: number }[] {
  return table.headers.map((header, index) => {
    const longest = [table.rows, table.total === undefined ? [] : [table.total]]
      .flat()
      .reduce((max, row) => {
        const cell = row[index];
        return cell === undefined ? max : Math.max(max, formatCell(cell).length);
      }, header.length);
    return {
      width: Math.min(MAX_COLUMN_WIDTH, Math.max(MIN_COLUMN_WIDTH, longest + WIDTH_PADDING)),
    };
  });
}

/** Her tablo bir sayfa; başlık satırı dondurulur. */
export async function buildWorkbookBlob(tables: ReportTable[]): Promise<Blob> {
  const sheets = tables.map((table) => ({
    sheet: sheetName(table.title),
    data: toSheetData(table),
    columns: columnWidths(table),
    stickyRowsCount: 1,
  }));
  return writeXlsxFile(sheets).toBlob();
}

export async function downloadWorkbook(tables: ReportTable[], fileName: string): Promise<void> {
  const blob = await buildWorkbookBlob(tables);
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = fileName;
  anchor.click();
  URL.revokeObjectURL(url);
}

export function reportFileName(from: string, to: string, extension: string): string {
  return `rapor_${from}_${to}.${extension}`;
}
