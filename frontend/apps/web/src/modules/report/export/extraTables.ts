import type { ShipmentRowView, SubcontractorBalanceView } from '@iplik/api-client';
import { formatDateTime } from '@iplik/i18n';
import type { ReportCell, ReportMeta, ReportTable } from './reportModel';
import { count, kg, optionalKg, sumKg, text } from './reportModel';

type Translate = (key: string) => string;

function dateOrEmpty(value: string | undefined): ReportCell {
  return text(value === undefined ? '' : formatDateTime(value));
}

/** Sevkiyat dökümü: satır başına bir sevkiyat; toplam satırı kiloları toplar. */
export function shipmentsTable(
  rows: ShipmentRowView[],
  meta: ReportMeta,
  t: Translate,
): ReportTable {
  const visible = meta.onlyDiscrepant ? rows.filter((row) => row.hadDiscrepancy) : rows;
  return {
    title: t('report.export.sheets.shipments'),
    headers: [
      t('report.export.shipmentNo'),
      t('report.parties.subcontractor'),
      t('report.export.status'),
      t('report.dashboard.dispatchNet'),
      t('report.dashboard.deliveryNet'),
      t('report.export.hadDiscrepancy'),
      t('report.export.createdAt'),
      t('report.export.completedAt'),
    ],
    rows: visible.map((row) => [
      text(row.shipmentNo),
      text(meta.nameOf(row.subcontractorId)),
      text(t(`shipment.status.${row.status}`)),
      optionalKg(row.dispatchNetKg),
      optionalKg(row.deliveryNetKg),
      text(row.hadDiscrepancy ? t('report.export.yes') : t('report.export.no')),
      dateOrEmpty(row.createdAt),
      dateOrEmpty(row.completedAt),
    ]),
    ...(visible.length === 0
      ? {}
      : {
          total: [
            text(t('report.export.total')),
            text(''),
            count(visible.length),
            kg(sumKg(visible.map((row) => row.dispatchNetKg))),
            kg(sumKg(visible.map((row) => row.deliveryNetKg))),
            count(visible.filter((row) => row.hadDiscrepancy).length),
            text(''),
            text(''),
          ],
        }),
  };
}

/** Fasoncu bakiyesi: gönderilen, yolda kalan ve teslim alınan kilolar. */
export function balanceTable(
  rows: SubcontractorBalanceView[],
  meta: ReportMeta,
  t: Translate,
): ReportTable {
  return {
    title: t('report.export.sheets.balance'),
    headers: [
      t('report.parties.subcontractor'),
      t('report.parties.shipments'),
      t('report.export.dispatched'),
      t('report.export.delivered'),
      t('report.export.inTransit'),
    ],
    rows: rows.map((row) => [
      text(meta.nameOf(row.subcontractorId)),
      count(row.shipmentCount),
      kg(row.dispatchedKg),
      kg(row.deliveredKg),
      kg(row.inTransitKg),
    ]),
    ...(rows.length === 0
      ? {}
      : {
          total: [
            text(t('report.export.total')),
            count(rows.reduce((sum, row) => sum + row.shipmentCount, 0)),
            kg(sumKg(rows.map((row) => row.dispatchedKg))),
            kg(sumKg(rows.map((row) => row.deliveredKg))),
            kg(sumKg(rows.map((row) => row.inTransitKg))),
          ],
        }),
  };
}
