import { formatKgNumber } from '@iplik/units';
import type { ReportCell } from './reportModel';

const PERCENT_SCALE = 100;

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
      return formatKgNumber(cell.value);
    case 'percent':
      return `%${percentFormat.format(cell.value * PERCENT_SCALE)}`;
  }
}

/** Excel sayı biçimi kodları. */
export const EXCEL_FORMATS = {
  count: '0',
  percent: '0.00%',
} as const;

const KG_MAX_DECIMALS = 3;
const KG_ROUND_FACTOR = 1000; // 10 üzeri KG_MAX_DECIMALS

/** Excel kilo biçimi hücrenin değerine göre seçilir: gereksiz sıfır yok (700, 264,5, 2.143,814), ekranla aynı. */
export function excelKgFormat(value: number): string {
  const rounded = Math.round(value * KG_ROUND_FACTOR) / KG_ROUND_FACTOR;
  const decimals = Math.min(KG_MAX_DECIMALS, (String(rounded).split('.')[1] ?? '').length);
  return decimals === 0 ? '#,##0' : `#,##0.${'0'.repeat(decimals)}`;
}
