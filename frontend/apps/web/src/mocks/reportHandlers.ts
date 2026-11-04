import type { DashboardView, PairStatsView, PartyStatsView, Permission } from '@iplik/api-client';
import { REPORTS_PATH } from '@iplik/api-client';
import { http, HttpResponse } from 'msw';

const MOCK_DASHBOARD: DashboardView = {
  from: '2026-10-05',
  to: '2026-11-03',
  shipments: { total: 12, inProgress: 4, inReview: 2, completed: 5, cancelled: 1 },
  quantity: {
    dispatchNetKg: '24000.000',
    deliveryNetKg: '23820.500',
    absoluteDiffKg: '310.500',
    comparedShipments: 7,
  },
  bands: { green: 4, yellow: 2, red: 1 },
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
  largestDifferences: [
    {
      shipmentId: 'shp-0007',
      shipmentNo: 'SV-2026-0007',
      diffKg: '180.000',
      diffPct: '3.60',
      band: 'RED',
    },
  ],
};

const MOCK_DRIVERS: PartyStatsView[] = [
  {
    id: 'a1b2c3d4-0000-7000-8000-000000000001',
    shipmentCount: 4,
    discrepantShipmentCount: 2,
    discrepancyRate: '50.00',
    averageAbsoluteDiffKg: '62.500',
  },
  {
    id: 'b2c3d4e5-0000-7000-8000-000000000002',
    shipmentCount: 3,
    discrepantShipmentCount: 0,
    discrepancyRate: '0.00',
    averageAbsoluteDiffKg: '4.000',
  },
];

const MOCK_PAIRS: PairStatsView[] = [
  {
    driverId: 'a1b2c3d4-0000-7000-8000-000000000001',
    subcontractorId: 'f9e8d7c6-0000-7000-8000-000000000009',
    shipmentCount: 4,
    discrepantShipmentCount: 2,
    discrepancyCount: 3,
    discrepancyRate: '50.00',
    averageAbsoluteDiffKg: '62.500',
  },
];

/** Raporlar yalnızca REPORT_VIEW izniyle; backend gibi izinsizse 403 döner. */
export function createReportHandlers(permissions: readonly Permission[]) {
  const canView = permissions.includes('REPORT_VIEW');
  const denied = () =>
    HttpResponse.json(
      { status: 403, code: 'ACCESS_DENIED', title: 'ACCESS_DENIED' },
      { status: 403 },
    );
  return [
    http.get(`${REPORTS_PATH}/dashboard`, () =>
      canView ? HttpResponse.json(MOCK_DASHBOARD) : denied(),
    ),
    http.get(`${REPORTS_PATH}/drivers`, () =>
      canView ? HttpResponse.json(MOCK_DRIVERS) : denied(),
    ),
    http.get(`${REPORTS_PATH}/subcontractors`, () =>
      canView ? HttpResponse.json(MOCK_DRIVERS) : denied(),
    ),
    http.get(`${REPORTS_PATH}/pairs`, () => (canView ? HttpResponse.json(MOCK_PAIRS) : denied())),
  ];
}
