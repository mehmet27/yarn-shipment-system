import type {
  DiscrepancyActivityView,
  DiscrepancyStatus,
  DiscrepancyView,
  ResolveDiscrepancyRequest,
  Severity,
} from '@iplik/api-client';

const COUNT = 25;
/** Backend'in sistem adımları için kullandığı aktör (sıfır UUID). */
const SYSTEM_ACTOR_ID = '00000000-0000-0000-0000-000000000000';
const DAY_MS = 86_400_000;
const BASE_TIME = Date.parse('2026-10-20T08:00:00Z');
const STATUSES: readonly DiscrepancyStatus[] = ['OPEN', 'OPEN', 'IN_REVIEW', 'RESOLVED', 'CLOSED'];

/** Backend yalnızca kalite itirazı üretir (QualityClaimWriter): kural QUALITY_CLAIM, değer "45.500 kg". */
const FACTS: Pick<DiscrepancyView, 'ruleCode' | 'actualValue'> = {
  ruleCode: 'QUALITY_CLAIM',
  actualValue: '45.500 kg',
};

function severityOf(index: number): Severity {
  return index % 3 === 0 ? 'RED' : 'YELLOW';
}

function create(index: number): DiscrepancyView {
  const status = STATUSES[index % STATUSES.length] ?? 'OPEN';
  const no = String(index + 1).padStart(4, '0');
  return {
    id: `disc-${String(index)}`,
    discrepancyNo: `UY-2026-${no}`,
    siteId: 'site-merkez',
    shipmentId: `shipment-${String(index)}`,
    type: 'QUALITY',
    severity: severityOf(index),
    status,
    ...FACTS,
    dueAt: new Date(BASE_TIME + (index + 5) * DAY_MS).toISOString(),
    createdAt: new Date(BASE_TIME + index * DAY_MS).toISOString(),
    ...(status === 'RESOLVED' || status === 'CLOSED'
      ? { resolution: 'TOLERANCE' as const, resolutionNote: 'Tolerans içinde kabul edildi.' }
      : {}),
  };
}

const items: DiscrepancyView[] = Array.from({ length: COUNT }, (_, index) => create(index));
const activity = new Map<string, DiscrepancyActivityView[]>();

type LogEntry = { actionType: string; actorId: string; note?: string };

function log(id: string, { actionType, actorId, note }: LogEntry) {
  const list = activity.get(id) ?? [];
  list.push({
    id: `${id}-act-${String(list.length)}`,
    actionType,
    actorId,
    occurredAt: new Date().toISOString(),
    ...(note === undefined ? {} : { note }),
  });
  activity.set(id, list);
}

export function listMockDiscrepancies(filter: {
  status?: string | null;
  severity?: string | null;
  overdue?: boolean;
}): DiscrepancyView[] {
  return items.filter(
    (item) =>
      (!filter.status || item.status === filter.status) &&
      (!filter.severity || item.severity === filter.severity) &&
      (!filter.overdue ||
        ((item.status === 'OPEN' || item.status === 'IN_REVIEW') &&
          item.dueAt !== undefined &&
          new Date(item.dueAt).getTime() < Date.now())),
  );
}

export function findMockDiscrepancy(id: unknown): DiscrepancyView | undefined {
  return items.find((item) => item.id === id);
}

export function mockActivity(id: string): DiscrepancyActivityView[] {
  return [
    {
      id: `${id}-act-open`,
      actionType: 'OPENED',
      actorId: SYSTEM_ACTOR_ID,
      note: findMockDiscrepancy(id)?.ruleCode ?? '',
      occurredAt: findMockDiscrepancy(id)?.createdAt ?? '',
    },
    ...(activity.get(id) ?? []),
  ];
}

export function assignMock(item: DiscrepancyView, assigneeId: string): void {
  item.assigneeId = assigneeId;
  item.status = 'IN_REVIEW';
  log(item.id, { actionType: 'ASSIGNED', actorId: assigneeId });
}

export function resolveMock(
  item: DiscrepancyView,
  request: ResolveDiscrepancyRequest,
  actorId: string,
): void {
  item.status = 'RESOLVED';
  item.resolution = request.resolution;
  item.resolutionNote = request.note;
  if (request.liableParty !== undefined) item.liableParty = request.liableParty;
  item.resolvedBy = actorId;
  item.resolvedAt = new Date().toISOString();
  log(item.id, { actionType: 'RESOLVED', actorId, note: request.note });
}
