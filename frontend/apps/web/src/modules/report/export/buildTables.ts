import type { PartyStatsView } from '@iplik/api-client';
import { formatDateTime } from '@iplik/i18n';
import { balanceTable, shipmentsTable } from './extraTables';
import type { ReportBundle, ReportCell, ReportMeta, ReportTable } from './reportModel';
import { count, filterRows, kg, partyTotals, percent, text } from './reportModel';

type Translate = (key: string) => string;

type PartyTableSpec = {
  title: string;
  partyHeaders: string[];
  rows: { parties: string[]; stats: PartyStatsView }[];
};

function partyTable({ title, partyHeaders, rows }: PartyTableSpec, t: Translate): ReportTable {
  const totals = partyTotals(rows.map((row) => row.stats));
  return {
    title,
    headers: [
      ...partyHeaders,
      t('report.parties.shipments'),
      t('report.parties.discrepant'),
      t('report.parties.rate'),
    ],
    rows: rows.map(({ parties, stats }) => [
      ...parties.map(text),
      count(stats.shipmentCount),
      count(stats.discrepantShipmentCount),
      percent(stats.discrepancyRate),
    ]),
    ...(rows.length === 0
      ? {}
      : {
          total: [
            text(t('report.export.total')),
            ...partyHeaders.slice(1).map(() => text('')),
            count(totals.shipments),
            count(totals.discrepant),
            percent(totals.rate),
          ],
        }),
  };
}

/** Özet sayfası: dönem bilgisi, sayılar ve dikkat listeleri tek tabloda anahtar/değer olarak. */
function summaryTable(bundle: ReportBundle, meta: ReportMeta, t: Translate): ReportTable {
  const { shipments, quantity, discrepancies } = bundle.dashboard;
  const pairs: [string, ReportCell][] = [
    [t('report.export.period'), text(`${meta.from} – ${meta.to}`)],
    [t('report.export.generatedAt'), text(formatDateTime(meta.generatedAt.toISOString()))],
    [t('report.export.generatedBy'), text(meta.generatedBy)],
    [
      t('report.export.filter'),
      text(meta.onlyDiscrepant ? t('report.export.onlyDiscrepant') : t('report.export.allRows')),
    ],
    [t('report.dashboard.total'), count(shipments.total)],
    [t('report.dashboard.inProgress'), count(shipments.inProgress)],
    [t('report.dashboard.inReview'), count(shipments.inReview)],
    [t('report.dashboard.completed'), count(shipments.completed)],
    [t('report.export.cancelled'), count(shipments.cancelled)],
    [t('report.dashboard.dispatchNet'), kg(quantity.dispatchNetKg)],
    [t('report.dashboard.deliveryNet'), kg(quantity.deliveryNetKg)],
    [t('report.dashboard.open'), count(discrepancies.open)],
    [t('report.dashboard.openRed'), count(discrepancies.red)],
    [t('report.dashboard.openYellow'), count(discrepancies.yellow)],
    [t('report.dashboard.overdue'), count(discrepancies.overdue)],
  ];
  return {
    title: t('report.export.sheets.summary'),
    headers: [t('report.export.indicator'), t('report.export.value')],
    rows: pairs.map(([label, cell]) => [text(label), cell]),
  };
}

function overdueTable(bundle: ReportBundle, t: Translate): ReportTable {
  return {
    title: t('report.export.sheets.overdue'),
    headers: [
      t('report.export.discrepancyNo'),
      t('report.export.severity'),
      t('report.export.dueAt'),
    ],
    rows: bundle.dashboard.overdueDiscrepancies.map((item) => [
      text(item.discrepancyNo),
      text(t(`discrepancy.severity.${item.severity}`)),
      text(formatDateTime(item.dueAt)),
    ]),
  };
}

/** Excel sayfaları ve yazdırma bölümleri aynı tablolardan üretilir; iki çıktı hiç ayrışmaz. */
export function buildTables(
  bundle: ReportBundle,
  meta: ReportMeta,
  translate: Translate,
): ReportTable[] {
  const t = translate;
  const subcontractorRows = filterRows(bundle.subcontractors, meta.onlyDiscrepant).map((stats) => ({
    parties: [meta.nameOf(stats.id)],
    stats,
  }));
  return [
    summaryTable(bundle, meta, t),
    overdueTable(bundle, t),
    partyTable(
      {
        title: t('report.export.sheets.subcontractors'),
        partyHeaders: [t('report.parties.subcontractor')],
        rows: subcontractorRows,
      },
      t,
    ),
    shipmentsTable(bundle.shipments, meta, t),
    balanceTable(bundle.balances, meta, t),
  ];
}
