import type { Permission, ResolveDiscrepancyRequest } from '@iplik/api-client';
import { DISCREPANCIES_PATH, Resolution } from '@iplik/api-client';
import { http, HttpResponse } from 'msw';
import {
  assignMock,
  findMockDiscrepancy,
  listMockDiscrepancies,
  mockActivity,
  resolveMock,
} from './discrepancies';

const DEFAULT_PAGE_SIZE = 20;
const problem = (status: number, code: string) =>
  HttpResponse.json({ status, code, title: code }, { status });
const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null;

function readResolve(json: unknown): ResolveDiscrepancyRequest | undefined {
  if (!isRecord(json)) return undefined;
  const { resolution, note, liableParty } = json;
  const known = Object.values(Resolution).find((value) => value === resolution);
  if (known === undefined || typeof note !== 'string' || note === '') return undefined;
  return typeof liableParty === 'string'
    ? { resolution: known, note, liableParty }
    : { resolution: known, note };
}

/** Uyuşmazlık uçları; karar eylemleri yalnızca DISCREPANCY_RESOLVE izniyle ve açık uyuşmazlıkta görünür. */
export function createDiscrepancyHandlers(permissions: readonly Permission[], userId: string) {
  const canView = permissions.includes('DISCREPANCY_VIEW');
  const canResolve = permissions.includes('DISCREPANCY_RESOLVE');
  const view = (id: unknown) => {
    const item = findMockDiscrepancy(id);
    if (item === undefined) return undefined;
    const isDecidable = canResolve && (item.status === 'OPEN' || item.status === 'IN_REVIEW');
    return { ...item, allowedActions: isDecidable ? (['ASSIGN', 'RESOLVE'] as const) : [] };
  };
  return [
    http.get(DISCREPANCIES_PATH, ({ request }) => {
      if (!canView) return problem(403, 'ACCESS_DENIED');
      const url = new URL(request.url);
      const page = Number(url.searchParams.get('page') ?? '0');
      const size = Number(url.searchParams.get('size') ?? String(DEFAULT_PAGE_SIZE));
      const matching = listMockDiscrepancies({
        status: url.searchParams.get('status'),
        severity: url.searchParams.get('severity'),
      });
      return HttpResponse.json({
        items: matching.slice(page * size, (page + 1) * size).map((item) => view(item.id)),
        page: {
          number: page,
          size,
          totalElements: matching.length,
          totalPages: Math.ceil(matching.length / size),
        },
      });
    }),
    http.get(`${DISCREPANCIES_PATH}/:id`, ({ params }) => {
      if (!canView) return problem(403, 'ACCESS_DENIED');
      const found = view(params.id);
      return found === undefined ? problem(404, 'DISCREPANCY_NOT_FOUND') : HttpResponse.json(found);
    }),
    http.get(`${DISCREPANCIES_PATH}/:id/activity`, ({ params }) =>
      !canView || findMockDiscrepancy(params.id) === undefined
        ? problem(404, 'DISCREPANCY_NOT_FOUND')
        : HttpResponse.json(mockActivity(String(params.id))),
    ),
    http.post(`${DISCREPANCIES_PATH}/:id/assign`, ({ params }) => {
      const item = findMockDiscrepancy(params.id);
      if (!canResolve) return problem(403, 'ACCESS_DENIED');
      if (item === undefined) return problem(404, 'DISCREPANCY_NOT_FOUND');
      if (item.status !== 'OPEN' && item.status !== 'IN_REVIEW') {
        return problem(409, 'DISCREPANCY_NOT_OPEN');
      }
      assignMock(item, userId);
      return HttpResponse.json(view(item.id));
    }),
    http.post(`${DISCREPANCIES_PATH}/:id/resolve`, async ({ params, request }) => {
      const item = findMockDiscrepancy(params.id);
      if (!canResolve) return problem(403, 'ACCESS_DENIED');
      if (item === undefined) return problem(404, 'DISCREPANCY_NOT_FOUND');
      const body = readResolve(await request.json());
      if (body === undefined) return problem(400, 'INVALID_REQUEST');
      if (item.status !== 'OPEN' && item.status !== 'IN_REVIEW') {
        return problem(409, 'DISCREPANCY_NOT_OPEN');
      }
      resolveMock(item, body, userId);
      return HttpResponse.json(view(item.id));
    }),
  ];
}
