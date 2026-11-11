import type { Content, TableCell, TDocumentDefinitions } from 'pdfmake/interfaces';
import { formatCell } from './formatCell';
import type { ReportCell, ReportTable } from './reportModel';

/** PDF'in sabit metinleri; ekran dili çağıran tarafta çözülür. */
export type PdfOptions = {
  title: string;
  intro: string;
  /** Tablo kaydı yoksa gösterilen metin */
  empty: string;
  /** Hazırlayan, kontrol eden, onaylayan */
  signatures: string[];
  dateLine: string;
  /** Veri URL'si olarak PNG/JPEG logo */
  logo: string | null;
};

const PAGE_MARGIN = 28;
const TABLE_FONT_SIZE = 8;
const SIGNATURE_LINE_LENGTH = 200;
const SIGNATURE_GAP = 40;
const LOGO_WIDTH = 160;
const LOGO_HEIGHT = 48;
const LOGO_FIT: [number, number] = [LOGO_WIDTH, LOGO_HEIGHT];

function isNumeric(cell: ReportCell | undefined): boolean {
  return cell !== undefined && cell.kind !== 'text';
}

function cellOf(cell: ReportCell, isBold: boolean): TableCell {
  return {
    text: formatCell(cell),
    bold: isBold,
    alignment: isNumeric(cell) ? 'right' : 'left',
  };
}

function tableContent(table: ReportTable, empty: string): Content[] {
  const heading: Content = { text: table.title, style: 'section' };
  if (table.rows.length === 0) return [heading, { text: empty, fontSize: TABLE_FONT_SIZE }];
  const header: TableCell[] = table.headers.map((value) => ({
    text: value,
    bold: true,
    fillColor: 'gainsboro',
  }));
  const body: TableCell[][] = [
    header,
    ...table.rows.map((row) => row.map((cell) => cellOf(cell, false))),
    ...(table.total === undefined ? [] : [table.total.map((cell) => cellOf(cell, true))]),
  ];
  return [
    heading,
    {
      table: {
        headerRows: 1,
        widths: table.headers.map(() => '*'),
        body,
      },
      fontSize: TABLE_FONT_SIZE,
      layout: 'lightHorizontalLines',
    },
  ];
}

function signatureBlock(options: PdfOptions): Content {
  return {
    unbreakable: true,
    marginTop: SIGNATURE_GAP,
    columns: options.signatures.map((label) => ({
      stack: [
        { text: label, bold: true, fontSize: 9 },
        {
          marginTop: SIGNATURE_GAP,
          canvas: [
            {
              type: 'line',
              x1: 0,
              y1: 0,
              x2: SIGNATURE_LINE_LENGTH,
              y2: 0,
              lineWidth: 0.5,
            },
          ],
        },
        { text: options.dateLine, fontSize: 8 },
      ],
    })),
  };
}

/** Rapor tablolarından PDF belge tanımı: yatay A4, başlıklar her sayfada tekrarlanır, sonda imza alanları. */
export function buildPdfDefinition(
  tables: ReportTable[],
  options: PdfOptions,
): TDocumentDefinitions {
  return {
    pageSize: 'A4',
    pageOrientation: 'landscape',
    pageMargins: PAGE_MARGIN,
    defaultStyle: { font: 'Roboto', fontSize: 10 },
    styles: { section: { fontSize: 12, bold: true, marginTop: 12, marginBottom: 4 } },
    info: { title: options.title },
    content: [
      ...(options.logo === null ? [] : [{ image: options.logo, fit: LOGO_FIT, marginBottom: 8 }]),
      { text: options.title, fontSize: 16, bold: true },
      ...tables.flatMap((table) => tableContent(table, options.empty)),
      { text: options.intro, fontSize: 8, marginTop: 12 },
      signatureBlock(options),
    ],
  };
}

/** PDF'i tarayıcıda üretip indirir; kütüphane ve yazı tipi yalnızca ilk kullanımda yüklenir. */
export async function downloadPdf(
  tables: ReportTable[],
  options: PdfOptions,
  fileName: string,
): Promise<void> {
  const [pdfMake, fonts] = await Promise.all([
    import('pdfmake/build/pdfmake'),
    import('pdfmake/build/vfs_fonts'),
  ]);
  pdfMake.addVirtualFileSystem(fonts.default);
  await pdfMake.createPdf(buildPdfDefinition(tables, options)).download(fileName);
}
