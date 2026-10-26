import type { Permission } from '@iplik/api-client';
import { http, HttpResponse } from 'msw';
import type { MasterKind, MockRecord, MockValues } from './masterData';
import {
  createMasterRecord,
  isDuplicatePlate,
  listMasterRecords,
  matchesQuery,
  setMasterActive,
  updateMasterRecord,
} from './masterData';

const DEFAULT_PAGE_SIZE = 20;
const BASE = '/api/v1/master-data';

const PATHS: Readonly<Record<MasterKind, string>> = {
  subcontractor: 'subcontractors',
  driver: 'drivers',
  vehicle: 'vehicles',
  site: 'sites',
};

const problem = (status: number, code: string, title: string) =>
  HttpResponse.json({ status, code, title }, { status });

/** Sözleşmedeki görünüm alanları: fasoncu `title`, şoför `fullName`, araç `plateNo`, lokasyon `name`. */
function toView(kind: MasterKind, record: MockRecord) {
  const { id, label, isActive, subcontractorId } = record;
  switch (kind) {
    case 'subcontractor':
      return { id, title: label, active: isActive };
    case 'driver':
      return {
        id,
        fullName: label,
        active: isActive,
        ...(subcontractorId ? { subcontractorId } : {}),
      };
    case 'vehicle':
      return { id, plateNo: label, active: isActive };
    case 'site':
      return { id, name: label, active: isActive };
  }
}

const LABEL_FIELD: Readonly<Record<MasterKind, string>> = {
  subcontractor: 'title',
  driver: 'fullName',
  vehicle: 'plateNo',
  site: 'name',
};

function readValues(kind: MasterKind, json: unknown): MockValues | undefined {
  if (typeof json !== 'object' || json === null) return undefined;
  const body = json as Record<string, unknown>;
  const label = body[LABEL_FIELD[kind]];
  if (typeof label !== 'string' || label.trim() === '') return undefined;
  const owner = body.subcontractorId;
  return typeof owner === 'string' && kind === 'driver'
    ? { label, subcontractorId: owner }
    : { label };
}

function listHandler(kind: MasterKind, canManage: boolean) {
  return http.get(`${BASE}/${PATHS[kind]}`, ({ request }) => {
    const params = new URL(request.url).searchParams;
    const isIncludingInactive = params.get('status') === 'ALL';
    if (isIncludingInactive && !canManage) return problem(403, 'FORBIDDEN', 'Yetkiniz yok');
    const page = Number(params.get('page') ?? '0');
    const size = Number(params.get('size') ?? String(DEFAULT_PAGE_SIZE));
    const ownerId = params.get('subcontractorId');
    const matching = listMasterRecords(kind).filter(
      (record) =>
        (isIncludingInactive || record.isActive) &&
        matchesQuery(record.label, params.get('q')) &&
        (ownerId === null || record.subcontractorId === ownerId),
    );
    return HttpResponse.json({
      items: matching.slice(page * size, (page + 1) * size).map((record) => toView(kind, record)),
      page: {
        number: page,
        size,
        totalElements: matching.length,
        totalPages: Math.ceil(matching.length / size),
      },
    });
  });
}

/** Mock'ta bu giriş adı alınmış sayılır (409 yolunu denemek için). */
const TAKEN_USERNAME = 'admin';
const MOCK_TEMPORARY_PASSWORD = 'Gecici-Sifre-1';

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

function writeHandlers(kind: MasterKind, canManage: boolean) {
  const path = `${BASE}/${PATHS[kind]}`;
  const denied = () => problem(403, 'FORBIDDEN', 'Yetkiniz yok');
  const notFound = () => problem(404, 'MASTER_DATA_NOT_FOUND', 'Kayıt bulunamadı');
  return [
    http.post(path, async ({ request }) => {
      if (!canManage) return denied();
      const values = readValues(kind, await request.json());
      if (values === undefined) return problem(400, 'INVALID_MASTER_DATA', 'Geçersiz kayıt');
      if (kind === 'vehicle' && isDuplicatePlate(values.label)) {
        return problem(409, 'DUPLICATE_PLATE', 'Plaka kullanımda');
      }
      return HttpResponse.json(toView(kind, createMasterRecord(kind, values)), { status: 201 });
    }),
    http.put(`${path}/:id`, async ({ params, request }) => {
      if (!canManage) return denied();
      const values = readValues(kind, await request.json());
      if (values === undefined) return problem(400, 'INVALID_MASTER_DATA', 'Geçersiz kayıt');
      const id = String(params.id);
      if (kind === 'vehicle' && isDuplicatePlate(values.label, id)) {
        return problem(409, 'DUPLICATE_PLATE', 'Plaka kullanımda');
      }
      const updated = updateMasterRecord(kind, id, values);
      return updated === undefined ? notFound() : HttpResponse.json(toView(kind, updated));
    }),
    http.post(`${path}/:id/login-account`, async ({ request }) => {
      if (!canManage) return denied();
      const body: unknown = await request.json();
      const username = isRecord(body) && typeof body.username === 'string' ? body.username : '';
      if (username === '') return problem(400, 'INVALID_MASTER_DATA', 'Geçersiz kayıt');
      if (username === TAKEN_USERNAME) {
        return problem(409, 'LOGIN_ACCOUNT_EXISTS', 'Giriş adı alınmış');
      }
      return HttpResponse.json(
        { username, temporaryPassword: MOCK_TEMPORARY_PASSWORD },
        { status: 201 },
      );
    }),
    ...(['deactivate', 'activate'] as const).map((action) =>
      http.post(`${path}/:id/${action}`, ({ params }) => {
        if (!canManage) return denied();
        return setMasterActive(kind, String(params.id), action === 'activate')
          ? new HttpResponse(null, { status: 204 })
          : notFound();
      }),
    ),
  ];
}

const KINDS: readonly MasterKind[] = ['subcontractor', 'driver', 'vehicle', 'site'];

/** Ana veri liste ve yönetim uçları; yönetim yalnızca MASTER_DATA_MANAGE izniyle (403 aksi halde). */
export function createMasterDataHandlers(permissions: readonly Permission[]) {
  const canManage = permissions.includes('MASTER_DATA_MANAGE');
  return KINDS.flatMap((kind) => [listHandler(kind, canManage), ...writeHandlers(kind, canManage)]);
}
