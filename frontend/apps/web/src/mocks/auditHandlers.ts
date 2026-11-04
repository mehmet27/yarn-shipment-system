import type { Permission } from '@iplik/api-client';
import type { AuditRecordView } from '@iplik/api-client/audit';
import { AUDIT_LOG_PATH } from '@iplik/api-client/audit';
import { http, HttpResponse } from 'msw';

const BASE_TIME = Date.parse('2026-11-01T09:00:00Z');
const ENTITIES = ['Shipment', 'Evidence', 'Discrepancy'] as const;
const ACTIONS = ['shipment.dispatched', 'evidence.uploaded', 'discrepancy.opened'] as const;
const SERVICES = ['shipment-service', 'evidence-service', 'discrepancy-service'] as const;

const RECORDS: AuditRecordView[] = Array.from({ length: 25 }, (_, index) => ({
  id: 25 - index,
  occurredAt: new Date(BASE_TIME - index * 60_000).toISOString(),
  recordedAt: new Date(BASE_TIME - index * 60_000 + 1000).toISOString(),
  sourceService: SERVICES[index % 3] ?? 'shipment-service',
  eventId: `00000000-0000-7000-8000-${String(index).padStart(12, '0')}`,
  actor: { role: 'WAREHOUSE', userId: '00000000-0000-7000-8000-0000000000aa' },
  action: ACTIONS[index % 3] ?? 'shipment.dispatched',
  entity: {
    type: ENTITIES[index % 3] ?? 'Shipment',
    id: `1234abcd-0000-7000-8000-${String(index).padStart(12, '0')}`,
  },
}));

/** Denetim servisinin sahte uçları; yalnızca AUDIT_VIEW izni olan rol okuyabilir. */
export function createAuditHandlers(permissions: readonly Permission[]) {
  const canView = permissions.includes('AUDIT_VIEW');
  const forbidden = () =>
    HttpResponse.json(
      { status: 403, code: 'ACCESS_DENIED', title: 'ACCESS_DENIED' },
      { status: 403 },
    );
  return [
    http.get(AUDIT_LOG_PATH, ({ request }) => {
      if (!canView) return forbidden();
      const params = new URL(request.url).searchParams;
      const page = Number(params.get('page') ?? '0');
      const size = Number(params.get('size') ?? '20');
      const entityType = params.get('entityType');
      const action = params.get('action');
      const matching = RECORDS.filter(
        (record) =>
          (entityType === null || record.entity?.type === entityType) &&
          (action === null || record.action.includes(action)),
      );
      return HttpResponse.json({
        items: matching.slice(page * size, (page + 1) * size),
        page: {
          number: page,
          size,
          totalElements: matching.length,
          totalPages: Math.ceil(matching.length / size),
        },
      });
    }),
    http.get(`${AUDIT_LOG_PATH}/chain-checks`, () =>
      canView
        ? HttpResponse.json([
            { id: 1, checkedAt: '2026-11-04T02:00:00Z', fromId: 1, toId: 25, ok: true },
          ])
        : forbidden(),
    ),
  ];
}
