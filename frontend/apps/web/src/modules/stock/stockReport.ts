import { formatDateTime } from '@iplik/i18n';
import type { CentralStock } from '@iplik/api-client/central-stock';
import type { ReportTable } from '@/modules/report';
import { kg, text } from '@/modules/report';

type Translate = (key: string) => string;

/** Ekrandaki merkez stok ve hareketleri Excel/PDF indirmesi için tablo modeline çevirir. */
export function stockReportTables(stock: CentralStock, t: Translate): ReportTable[] {
  return [
    {
      title: t('stock.title'),
      headers: [t('stock.yarnType'), t('stock.stockKg')],
      rows: stock.rows.map((row) => [text(row.yarnType), kg(row.stockKg)]),
      total: [text(t('stock.total')), kg(stock.totalKg)],
    },
    {
      title: t('stock.movements'),
      headers: [
        t('stock.date'),
        t('stock.kind'),
        t('stock.yarnType'),
        t('stock.kg'),
        t('stock.note'),
      ],
      rows: stock.movements.map((move) => [
        text(formatDateTime(move.occurredAt)),
        text(t(`stock.kinds.${move.kind}`)),
        text(move.yarnType),
        kg(move.netKg),
        text(move.note ?? ''),
      ]),
    },
  ];
}
