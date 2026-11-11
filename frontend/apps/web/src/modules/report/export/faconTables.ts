import type { FireRowView, YarnVsFabricRowView } from '@iplik/api-client';
import { formatDateTime } from '@iplik/i18n';
import type { BalanceMatrix } from '../pivot';
import type { ReportCell, ReportTable } from './reportModel';
import { count, kg, percent, sumKg, text } from './reportModel';

type Translate = (key: string) => string;

const TOTAL_KEY = 'report.export.total';

/** Bakiye matrisi: iplik cinsi satırları, fasoncu sütunları, toplam sütunu, yolda sütunu ve toplam satırı. */
export function balanceMatrixTable(matrix: BalanceMatrix, t: Translate): ReportTable {
  return {
    title: t('report.export.sheets.balanceMatrix'),
    headers: [
      t('report.matrix.yarnType'),
      ...matrix.columns.map((column) => column.name),
      t(TOTAL_KEY),
      t('report.matrix.inTransit'),
    ],
    rows: matrix.rows.map((row) => [
      text(row.yarnType),
      ...matrix.columns.map((column) => kg(row.values[column.id] ?? 0)),
      kg(row.totalKg),
      kg(row.inTransitKg),
    ]),
    ...(matrix.rows.length === 0
      ? {}
      : {
          total: [
            text(t(TOTAL_KEY)),
            ...matrix.columns.map((column) => kg(matrix.columnTotals[column.id] ?? 0)),
            kg(matrix.grandTotalKg),
            kg(matrix.grandInTransitKg),
          ],
        }),
  };
}

/** Oranlar sunucudan kesir olarak gelir (0,04); hücre kesir tutar. */
const rate = (value: string): ReportCell => percent(Number(value) * 100);

const STATUS_KEY = {
  AWAITING_FABRIC: 'report.fire.status.AWAITING_FABRIC',
  TO_INVOICE: 'report.fire.status.TO_INVOICE',
  CLOSED: 'report.fire.status.CLOSED',
} as const;

function fireTotal(rows: FireRowView[], t: Translate, withStatus: boolean): ReportCell[] {
  return [
    text(t(TOTAL_KEY)),
    text(''),
    kg(sumKg(rows.map((row) => row.sentKg))),
    kg(sumKg(rows.map((row) => row.returnedYarnKg))),
    kg(sumKg(rows.map((row) => row.fabricKg))),
    text(''),
    kg(sumKg(rows.map((row) => row.expectedFabricKg))),
    text(''),
    kg(sumKg(rows.map((row) => row.shortageKg))),
    ...(withStatus ? [text(''), text(''), text('')] : []),
  ];
}

/** Fire raporu ve faturalanacaklar aynı satır şekliyle çalışır; durum/fatura sütunları yalnız fire raporunda. */
export function fireTable(
  rows: FireRowView[],
  t: Translate,
  options: { title: string; withStatus: boolean },
): ReportTable {
  return {
    title: options.title,
    headers: [
      t('report.export.shipmentNo'),
      t('report.parties.subcontractor'),
      t('report.fire.sent'),
      t('report.fire.returnedYarn'),
      t('report.fire.fabric'),
      t('report.fire.fireRate'),
      t('report.fire.expectedFabric'),
      t('report.fire.actualFireRate'),
      t('report.fire.shortage'),
      ...(options.withStatus
        ? [t('report.export.status'), t('report.fire.invoiceNo'), t('report.fire.invoicedAt')]
        : []),
    ],
    rows: rows.map((row) => [
      text(row.shipmentNo),
      text(row.subcontractorName),
      kg(row.sentKg),
      kg(row.returnedYarnKg),
      kg(row.fabricKg),
      rate(row.fireRate),
      kg(row.expectedFabricKg),
      rate(row.actualFireRate),
      kg(row.shortageKg),
      ...(options.withStatus
        ? [
            text(t(STATUS_KEY[row.status])),
            text(row.invoiceNo ?? ''),
            text(row.invoicedAt === undefined ? '' : formatDateTime(row.invoicedAt)),
          ]
        : []),
    ]),
    ...(rows.length === 0 ? {} : { total: fireTotal(rows, t, options.withStatus) }),
  };
}

/** Giden iplik ↔ gelen kumaş; toplamdaki gerçekleşen fire kiloların toplamından hesaplanır. */
export function yarnVsFabricTable(rows: YarnVsFabricRowView[], t: Translate): ReportTable {
  const sent = sumKg(rows.map((row) => row.sentKg));
  const returned = sumKg(rows.map((row) => row.returnedYarnKg));
  const fabric = sumKg(rows.map((row) => row.fabricKg));
  const base = sent - returned;
  return {
    title: t('report.export.sheets.yarnVsFabric'),
    headers: [
      t('report.parties.subcontractor'),
      t('report.fire.sent'),
      t('report.fire.returnedYarn'),
      t('report.fire.fabric'),
      t('report.fire.actualFireRate'),
      t('report.fire.shortage'),
      t('report.fire.overLimitCount'),
      t('report.fire.overLimitFire'),
    ],
    rows: rows.map((row) => [
      text(row.subcontractorName),
      kg(row.sentKg),
      kg(row.returnedYarnKg),
      kg(row.fabricKg),
      rate(row.actualFireRate),
      kg(row.shortageKg),
      count(row.overLimitShipmentCount),
      kg(row.overLimitFireKg),
    ]),
    ...(rows.length === 0
      ? {}
      : {
          total: [
            text(t(TOTAL_KEY)),
            kg(sent),
            kg(returned),
            kg(fabric),
            percent(base === 0 ? 0 : ((base - fabric) / base) * 100),
            kg(sumKg(rows.map((row) => row.shortageKg))),
            count(rows.reduce((sum, row) => sum + row.overLimitShipmentCount, 0)),
            kg(sumKg(rows.map((row) => row.overLimitFireKg))),
          ],
        }),
  };
}
