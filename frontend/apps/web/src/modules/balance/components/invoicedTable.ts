import type { FireInvoiceRow } from '@iplik/api-client/settlements';
import { formatDate } from '@iplik/i18n';
import type { ReportTable } from '@/modules/report';
import { kg, text } from '@/modules/report';

type Translate = (key: string) => string;

/** Faturalananlar Excel tablosu: ekrandaki sütunlar, kilo sayı olarak, sonda toplam kilo. */
export function invoicedTable(
  rows: readonly FireInvoiceRow[],
  totalKg: string,
  t: Translate,
): ReportTable {
  return {
    title: t('settlement.invoiced.sheet'),
    headers: [
      t('settlement.invoiced.invoiceNo'),
      t('settlement.invoiced.date'),
      t('settlement.invoiced.subcontractor'),
      t('settlement.invoiced.shipment'),
      t('settlement.invoiced.kg'),
    ],
    rows: rows.map((row) => [
      text(row.invoiceNo),
      text(formatDate(row.invoicedOn)),
      text(row.subcontractorTitle),
      text(row.shipmentNo),
      kg(row.kg),
    ]),
    ...(rows.length === 0
      ? {}
      : {
          total: [text(t('settlement.invoiced.total')), text(''), text(''), text(''), kg(totalKg)],
        }),
  };
}
