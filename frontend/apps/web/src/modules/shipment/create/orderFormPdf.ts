/* eslint-disable @typescript-eslint/no-magic-numbers -- PDF yerleşim ölçüleri Excel şablonundan alınmış sabitlerdir */
import type { Content, TableCell, TDocumentDefinitions } from 'pdfmake/interfaces';
import type { CreateDraft, RecipeLineDraft } from './createDraft';
import { parsePositiveKg, totalNetKg } from './createDraft';

/** PDF'teki sabit metinler (antet, açıklamalar, imza başlıkları); `create.pdf.*` çevirilerinden gelir. */
export type OrderFormTexts = {
  company: string;
  companyLine: string;
  address: string;
  firm: string;
  orderNo: string;
  contact: string;
  date: string;
  columns: readonly string[];
  total: string;
  notesTitle: string;
  notes: readonly string[];
  orderApproval: string;
  producer: string;
  approver: string;
  widthTypes: Readonly<Record<string, string>>;
};

const MIN_ROWS = 8;
const KG_FORMAT = new Intl.NumberFormat('tr-TR', { maximumFractionDigits: 3 });
const PRICE_FORMAT = new Intl.NumberFormat('tr-TR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});
const COLUMN_WIDTHS = ['*', 52, 56, 72, 46, 46, 50];
const SMALL = 7;

function kgText(value: string): string {
  const number = Number(value);
  return Number.isFinite(number) && number > 0 ? KG_FORMAT.format(number) : '';
}

function priceText(input: string): string {
  const text = input.trim();
  if (text === '') return '';
  const number = Number(text.replaceAll('.', '').replace(',', '.'));
  return Number.isFinite(number) ? `${PRICE_FORMAT.format(number)} TL` : text;
}

type CellStyle = { alignment?: 'left' | 'center'; color?: string; fontSize?: number };

function center(text: string, style: CellStyle = {}): TableCell {
  return { text, alignment: 'center', margin: [0, 6, 0, 6], bold: true, ...style };
}

function lineRow(line: RecipeLineDraft, texts: OrderFormTexts): TableCell[] {
  const parsed = parsePositiveKg(line.netKg);
  return [
    center(line.fabricType, { alignment: 'left' }),
    center(line.lycra, { color: 'red' }),
    center(texts.widthTypes[line.widthType] ?? ''),
    center(line.yarnLength),
    center(line.pussFine),
    center(parsed === undefined ? '' : kgText(parsed)),
    center(priceText(line.unitPrice)),
  ];
}

function blankRow(): TableCell[] {
  return COLUMN_WIDTHS.map(() => center(''));
}

function headerBlock(texts: OrderFormTexts): Content {
  return {
    table: {
      widths: [320, '*'],
      body: [
        [
          {
            stack: [
              { text: texts.company, fontSize: 30, bold: true },
              { text: texts.companyLine, fontSize: 6, bold: true },
            ],
            margin: [8, 4, 0, 4],
          },
          { text: texts.address, fontSize: 6.5, bold: true, margin: [4, 12, 4, 4] },
        ],
      ],
    },
  };
}

function infoBlock(draft: CreateDraft, texts: OrderFormTexts, date: string): Content {
  return {
    table: {
      widths: [320, '*', 140],
      body: [
        [
          {
            text: [{ text: `${texts.firm} : `, bold: true }, draft.subcontractor?.label ?? ''],
            margin: [2, 3, 0, 3],
          },
          { text: `${texts.orderNo} :  ${draft.orderNo}`, bold: true, margin: [2, 3, 0, 3] },
          { text: '' },
        ],
        [
          {
            text: [
              { text: `${texts.contact} : `, bold: true, fontSize: SMALL },
              draft.contactPerson,
            ],
            margin: [2, 5, 0, 5],
          },
          {
            text: `${texts.date} : ${date}`,
            bold: true,
            fontSize: 13,
            colSpan: 2,
            margin: [2, 2, 0, 2],
          },
          {},
        ],
      ],
    },
  };
}

function footerBlocks(texts: OrderFormTexts): Content[] {
  return [
    {
      table: {
        widths: ['*'],
        body: [
          [
            {
              stack: [
                { text: texts.notesTitle, fontSize: 6, bold: true },
                ...texts.notes.map((note) => ({ text: note, fontSize: 6, bold: true })),
              ],
              margin: [2, 2, 2, 2],
            },
          ],
        ],
      },
    },
    {
      table: {
        widths: ['*', '*', '*'],
        heights: [70],
        body: [
          [
            { text: texts.orderApproval, bold: true, fontSize: SMALL, margin: [2, 2, 0, 0] },
            { text: texts.producer, bold: true, fontSize: SMALL, margin: [2, 2, 0, 0] },
            {
              stack: [
                { text: texts.orderApproval, bold: true, fontSize: SMALL },
                { text: texts.approver, bold: true, fontSize: SMALL, marginTop: 8 },
              ],
              margin: [2, 2, 0, 0],
            },
          ],
        ],
      },
    },
  ];
}

/** Sipariş formu (Excel şablonunun birebir karşılığı): antet, firma bilgileri, satır tablosu, toplam, açıklamalar ve imzalar. */
export function buildOrderFormDefinition(
  draft: CreateDraft,
  texts: OrderFormTexts,
  date: string,
): TDocumentDefinitions {
  const rows = draft.lines.map((line) => lineRow(line, texts));
  const padding = Array.from({ length: Math.max(0, MIN_ROWS - rows.length) }, blankRow);
  const total = kgText(totalNetKg(draft.lines));
  return {
    pageSize: 'A4',
    pageMargins: 24,
    defaultStyle: { font: 'Roboto', fontSize: 9 },
    info: { title: `${texts.company} ${texts.orderNo}` },
    content: [
      headerBlock(texts),
      infoBlock(draft, texts, date),
      {
        table: {
          headerRows: 1,
          widths: COLUMN_WIDTHS,
          body: [
            texts.columns.map((name) => center(name, { fontSize: SMALL })),
            ...rows,
            ...padding,
            [
              { text: '', colSpan: 4, border: [false, false, false, false] },
              {},
              {},
              {},
              center(texts.total, { fontSize: 11 }),
              center(total, { fontSize: 13 }),
              center(''),
            ],
          ],
        },
      },
      ...footerBlocks(texts),
    ],
  };
}

/** PDF'i tarayıcıda üretip indirir; kütüphane ve yazı tipi yalnızca ilk kullanımda yüklenir. */
export async function downloadOrderForm(
  draft: CreateDraft,
  options: { texts: OrderFormTexts; date: string; fileName: string },
): Promise<void> {
  const { texts, date, fileName } = options;
  const [pdfMake, fonts] = await Promise.all([
    import('pdfmake/build/pdfmake'),
    import('pdfmake/build/vfs_fonts'),
  ]);
  pdfMake.addVirtualFileSystem(fonts.default);
  await pdfMake.createPdf(buildOrderFormDefinition(draft, texts, date)).download(fileName);
}
/* eslint-enable @typescript-eslint/no-magic-numbers -- ölçü sabitleri bölümü sonu */
