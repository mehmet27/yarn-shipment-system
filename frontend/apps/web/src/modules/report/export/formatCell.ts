import type { ReportCell } from './reportModel';

const PERCENT_SCALE = 100;

const kgFormat = new Intl.NumberFormat('tr-TR', {
  minimumFractionDigits: 3,
  maximumFractionDigits: 3,
});
const percentFormat = new Intl.NumberFormat('tr-TR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

/** Yazdırma/ekran metni; Excel'de ise hücre sayısal kalır ve biçimi aşağıdaki kodlarla verilir. */
export function formatCell(cell: ReportCell): string {
  switch (cell.kind) {
    case 'text':
      return cell.value;
    case 'count':
      return String(cell.value);
    case 'kg':
      return `${kgFormat.format(cell.value)} kg`;
    case 'percent':
      return `%${percentFormat.format(cell.value * PERCENT_SCALE)}`;
  }
}

/** Excel sayı biçimi kodları. */
export const EXCEL_FORMATS = {
  count: '0',
  kg: '#,##0.000',
  percent: '0.00%',
} as const;
