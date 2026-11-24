import type {
  AmountSummaryView,
  BalanceMatrixCell,
  DashboardView,
  FireRowView,
  MonthlyFlowView,
  PartyStatsView,
  Permission,
  ShipmentRowView,
  SubcontractorBalanceView,
  YarnVsFabricRowView,
} from '@iplik/api-client';
import { REPORTS_PATH } from '@iplik/api-client';
import { todayIsoDate } from '@iplik/i18n';
import { http, HttpResponse } from 'msw';

const MOCK_DASHBOARD: DashboardView = {
  from: '2026-10-05',
  to: '2026-11-03',
  shipments: { total: 12, inProgress: 4, inReview: 2, completed: 5, cancelled: 1 },
  quantity: {
    dispatchNetKg: '24000.000',
    deliveryNetKg: '23820.500',
  },
  discrepancies: { open: 3, red: 1, yellow: 2, overdue: 1 },
  overdueDiscrepancies: [
    {
      discrepancyId: 'disc-0',
      discrepancyNo: 'UY-2026-0001',
      shipmentId: 'shp-0007',
      severity: 'RED',
      dueAt: '2026-11-01T09:00:00Z',
    },
  ],
};

/** Bugünden önce biten dönem "önceki dönem" sayılır; kıyas satırları mock'ta da anlamlı görünür. */
const MOCK_PREVIOUS_DASHBOARD: DashboardView = {
  ...MOCK_DASHBOARD,
  shipments: { total: 10, inProgress: 0, inReview: 1, completed: 8, cancelled: 1 },
  quantity: { dispatchNetKg: '30000.000', deliveryNetKg: '23820.500' },
};

const MOCK_AMOUNTS: AmountSummaryView = {
  shippedAmount: '412300.00',
  shippedUnpricedKg: '0.000',
  toInvoiceAmount: '96400.00',
  toInvoiceUnpricedKg: '0.000',
  subcontractorYarnAmount: '505000.00',
  subcontractorUnpricedKg: '120.000',
  subcontractors: [
    {
      subcontractorId: 'a1b2c3d4-0000-7000-8000-000000000001',
      subcontractorName: 'Yıldız Triko',
      balanceKg: '2940.000',
      amount: '294000.00',
      unpricedKg: '0.000',
    },
    {
      subcontractorId: 'b2c3d4e5-0000-7000-8000-000000000002',
      subcontractorName: 'Ay Örme',
      balanceKg: '2230.000',
      amount: '211000.00',
      unpricedKg: '120.000',
    },
  ],
};

const MOCK_SUBCONTRACTORS: PartyStatsView[] = [
  {
    id: 'a1b2c3d4-0000-7000-8000-000000000001',
    shipmentCount: 4,
    discrepantShipmentCount: 2,
    discrepancyRate: '50.00',
  },
  {
    id: 'b2c3d4e5-0000-7000-8000-000000000002',
    shipmentCount: 3,
    discrepantShipmentCount: 0,
    discrepancyRate: '0.00',
  },
];

const MOCK_SHIPMENTS: ShipmentRowView[] = [
  {
    shipmentId: 'shp-0007',
    shipmentNo: 'SV-2026-0007',
    subcontractorId: 'f9e8d7c6-0000-7000-8000-000000000009',
    status: 'COMPLETED',
    dispatchNetKg: '5000.000',
    deliveryNetKg: '4820.000',
    hadDiscrepancy: true,
    createdAt: '2026-11-02T09:00:00Z',
  },
  {
    shipmentId: 'shp-0008',
    shipmentNo: 'SV-2026-0008',
    subcontractorId: 'f9e8d7c6-0000-7000-8000-000000000009',
    status: 'AWAITING_DELIVERY',
    dispatchNetKg: '2000.000',
    hadDiscrepancy: false,
    createdAt: '2026-11-03T09:00:00Z',
  },
];

const MOCK_BALANCES: SubcontractorBalanceView[] = [
  {
    subcontractorId: 'f9e8d7c6-0000-7000-8000-000000000009',
    shipmentCount: 2,
    dispatchedKg: '7000.000',
    deliveredKg: '4820.000',
    inTransitKg: '2000.000',
  },
];

const SUB_A = 'f9e8d7c6-0000-7000-8000-000000000009';
const MOCK_MATRIX: BalanceMatrixCell[] = [
  {
    yarnType: '30/1 Penye',
    subcontractorId: SUB_A,
    subcontractorName: 'Kırıkhan Tekstil',
    balanceKg: '1982.500',
    inTransitKg: '500.000',
  },
  {
    yarnType: '30/1 Penye',
    subcontractorId: 'sub-b',
    subcontractorName: 'Aksu Dokuma',
    balanceKg: '1000.000',
    inTransitKg: '0.000',
  },
  {
    yarnType: '20/1 Süprem',
    subcontractorId: SUB_A,
    subcontractorName: 'Kırıkhan Tekstil',
    balanceKg: '750.250',
    inTransitKg: '0.000',
  },
];
const MOCK_FIRE: FireRowView[] = [
  {
    shipmentId: 'shp-0007',
    shipmentNo: 'SV-2026-0007',
    subcontractorId: SUB_A,
    subcontractorName: 'Kırıkhan Tekstil',
    sentKg: '1000.000',
    returnedYarnKg: '0.000',
    fabricKg: '900.000',
    fireRate: '0.04',
    expectedFabricKg: '960.000',
    actualFireRate: '0.10',
    shortageKg: '60.000',
    status: 'TO_INVOICE',
  },
  {
    shipmentId: 'shp-0008',
    shipmentNo: 'SV-2026-0008',
    subcontractorId: 'sub-b',
    subcontractorName: 'Aksu Dokuma',
    sentKg: '2000.000',
    returnedYarnKg: '0.000',
    fabricKg: '1920.000',
    fireRate: '0.04',
    expectedFabricKg: '1920.000',
    actualFireRate: '0.04',
    shortageKg: '0.000',
    status: 'CLOSED',
    invoiceNo: 'F-1',
    invoicedAt: '2026-11-05T09:00:00Z',
  },
];
const MOCK_YARN_VS_FABRIC: YarnVsFabricRowView[] = [
  {
    subcontractorId: SUB_A,
    subcontractorName: 'Kırıkhan Tekstil',
    sentKg: '1000.000',
    returnedYarnKg: '0.000',
    fabricKg: '900.000',
    actualFireRate: '0.10',
    shortageKg: '60.000',
    overLimitShipmentCount: 1,
    overLimitFireKg: '100.000',
  },
];

const month = (name: string, sentKg: string, fabricKg: string): MonthlyFlowView => ({
  month: name,
  from: `${name}-01`,
  to: `${name}-28`,
  shipmentCount: sentKg === '0.000' ? 0 : 1,
  sentKg,
  returnedYarnKg: '0.000',
  fabricKg,
  actualFireRate: sentKg === '0.000' ? '0.0000' : '0.0500',
  shortageKg: '0.000',
});
/** Son altı ay; son ay "Giden iplik ↔ gelen kumaş" sahte satırının toplamıyla aynıdır. */
const MOCK_TREND: MonthlyFlowView[] = [
  month('2026-06', '0.000', '0.000'),
  month('2026-07', '18500.000', '17575.000'),
  month('2026-08', '22340.500', '21223.475'),
  month('2026-09', '19800.000', '18810.000'),
  month('2026-10', '24000.000', '22800.000'),
  { ...month('2026-11', '1000.000', '900.000'), actualFireRate: '0.1000', shortageKg: '60.000' },
];

/** Raporlar yalnızca REPORT_VIEW izniyle; backend gibi izinsizse 403 döner. */
export function createReportHandlers(permissions: readonly Permission[]) {
  const canView = permissions.includes('REPORT_VIEW');
  const canSeeAmounts = permissions.includes('AMOUNT_VIEW');
  const denied = () =>
    HttpResponse.json(
      { status: 403, code: 'ACCESS_DENIED', title: 'ACCESS_DENIED' },
      { status: 403 },
    );
  return [
    http.get(`${REPORTS_PATH}/dashboard`, ({ request }) => {
      if (!canView) return denied();
      const to = new URL(request.url).searchParams.get('to');
      const isPast = to !== null && to < todayIsoDate();
      return HttpResponse.json(isPast ? MOCK_PREVIOUS_DASHBOARD : MOCK_DASHBOARD);
    }),
    http.get(`${REPORTS_PATH}/subcontractors`, () =>
      canView ? HttpResponse.json(MOCK_SUBCONTRACTORS) : denied(),
    ),
    http.get(`${REPORTS_PATH}/shipments`, () =>
      canView ? HttpResponse.json(MOCK_SHIPMENTS) : denied(),
    ),
    http.get(`${REPORTS_PATH}/subcontractor-balance`, () =>
      canView ? HttpResponse.json(MOCK_BALANCES) : denied(),
    ),
    http.get(`${REPORTS_PATH}/balance-matrix`, () =>
      canView ? HttpResponse.json(MOCK_MATRIX) : denied(),
    ),
    http.get(`${REPORTS_PATH}/fire`, () => (canView ? HttpResponse.json(MOCK_FIRE) : denied())),
    http.get(`${REPORTS_PATH}/yarn-vs-fabric`, () =>
      canView ? HttpResponse.json(MOCK_YARN_VS_FABRIC) : denied(),
    ),
    http.get(`${REPORTS_PATH}/monthly-trend`, () =>
      canView ? HttpResponse.json(MOCK_TREND) : denied(),
    ),
    http.get(`${REPORTS_PATH}/to-invoice`, () =>
      canView
        ? HttpResponse.json(MOCK_FIRE.filter((row) => row.status === 'TO_INVOICE'))
        : denied(),
    ),
    http.get(`${REPORTS_PATH}/amounts`, () =>
      canSeeAmounts ? HttpResponse.json(MOCK_AMOUNTS) : denied(),
    ),
    http.post(`${REPORTS_PATH}/balance/opening-import`, async ({ request }) => {
      const body = (await request.json()) as { rows: unknown[] };
      return canView ? HttpResponse.json({ imported: body.rows.length, skipped: 0 }) : denied();
    }),
  ];
}
