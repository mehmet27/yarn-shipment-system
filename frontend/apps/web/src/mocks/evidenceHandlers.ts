import type { EvidenceType, Permission } from '@iplik/api-client';
import { EvidenceType as EVIDENCE_TYPES } from '@iplik/api-client';
import { http, HttpResponse } from 'msw';
import { UPLOAD_PERMISSION_BY_TYPE } from '@/modules/evidence/uploadTypes';

const EVIDENCE_PATH = '/api/v1/evidence/uploads';
const STORAGE_URL = 'https://mock-storage.local/evidence';
const MAX_BYTES = 20 * 1024 * 1024;
const ALLOWED_MIME = ['image/jpeg', 'image/png', 'image/heic', 'application/pdf'];
const TICKET_TTL_MS = 600_000;
type Pending = {
  shipmentId: string;
  evidenceType: EvidenceType;
  mimeType: string;
  sizeBytes: number;
};

/** Fiş fotoğrafı yerine gösterilen küçük görsel; gerçek depo adresi geliştirmede açılamaz. */
const PHOTO_DATA_URL =
  'data:image/svg+xml,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="200" height="120"><rect width="200" height="120" fill="#eee"/><text x="20" y="65">Kantar fişi</text></svg>',
  );

type StoredEvidence = Pending & {
  id: string;
  status: 'VALID' | 'INVALIDATED';
  sha256: string;
  uploadedAt: string;
};

/** Geliştirmede örnek kanıtlar; yüklenenler de buraya eklenir. */
const stored = new Map<string, StoredEvidence>(
  ['shp-0001', 'shp-0007'].map((shipmentId, index) => [
    `00000000-0000-4000-8000-00000000000${String(index + 1)}`,
    {
      id: `00000000-0000-4000-8000-00000000000${String(index + 1)}`,
      shipmentId,
      evidenceType: 'DISPATCH_SCALE',
      mimeType: 'image/jpeg',
      sizeBytes: 1000,
      status: 'VALID',
      sha256: 'mock-sha256',
      uploadedAt: '2026-11-10T09:00:00Z',
    },
  ]),
);

const pending = new Map<string, Pending>();
const uploaded = new Set<string>();

const problem = (status: number, code: string) =>
  HttpResponse.json({ status, code, title: code }, { status });
const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null;

function readRequest(json: unknown): Pending | undefined {
  if (!isRecord(json)) return undefined;
  const { shipmentId, evidenceType, mimeType, sizeBytes } = json;
  const type = Object.values(EVIDENCE_TYPES).find((value) => value === evidenceType);
  const isValid =
    typeof shipmentId === 'string' &&
    type !== undefined &&
    typeof mimeType === 'string' &&
    ALLOWED_MIME.includes(mimeType) &&
    typeof sizeBytes === 'number' &&
    sizeBytes >= 1 &&
    sizeBytes <= MAX_BYTES;
  return isValid ? { shipmentId, evidenceType: type, mimeType, sizeBytes } : undefined;
}

/** Kanıt yükleme: izin iste → depoya PUT → onayla (backend sözleşmesiyle aynı üç adım). */
export function createEvidenceHandlers(permissions: readonly Permission[]) {
  return [
    http.post(EVIDENCE_PATH, async ({ request }) => {
      const body = readRequest(await request.json());
      if (body === undefined) return problem(400, 'UPLOAD_NOT_ALLOWED');
      if (!permissions.includes(UPLOAD_PERMISSION_BY_TYPE[body.evidenceType])) {
        return problem(400, 'UPLOAD_NOT_ALLOWED');
      }
      const evidenceId = crypto.randomUUID();
      pending.set(evidenceId, body);
      return HttpResponse.json(
        {
          evidenceId,
          uploadUrl: `${STORAGE_URL}/${evidenceId}`,
          // Backend imzaya boyutu da katar ve başlığı istemciye döner (S3ObjectStorageAdapter).
          headers: { 'Content-Type': body.mimeType, 'Content-Length': String(body.sizeBytes) },
          expiresAt: new Date(Date.now() + TICKET_TTL_MS).toISOString(),
        },
        { status: 201 },
      );
    }),
    http.put(`${STORAGE_URL}/:evidenceId`, ({ params }) => {
      uploaded.add(String(params.evidenceId));
      return new HttpResponse(null, { status: 200 });
    }),
    http.get('/api/v1/evidence/:evidenceId/download-url', () =>
      HttpResponse.json({
        url: PHOTO_DATA_URL,
        expiresAt: new Date(Date.now() + TICKET_TTL_MS).toISOString(),
      }),
    ),
    http.get('/api/v1/evidence', ({ request }) => {
      const shipmentId = new URL(request.url).searchParams.get('shipmentId');
      return HttpResponse.json({
        items: [...stored.values()].filter((item) => item.shipmentId === shipmentId),
      });
    }),
    http.get('/api/v1/evidence/:evidenceId', ({ params }) => {
      const found = stored.get(String(params.evidenceId));
      return found === undefined ? problem(404, 'EVIDENCE_NOT_FOUND') : HttpResponse.json(found);
    }),
    http.post(`${EVIDENCE_PATH}/:evidenceId/confirm`, ({ params }) => {
      const id = String(params.evidenceId);
      const ticket = pending.get(id);
      if (ticket === undefined) return problem(404, 'UPLOAD_NOT_FOUND');
      if (!uploaded.has(id)) return problem(409, 'UPLOAD_NOT_COMPLETED');
      const view: StoredEvidence = {
        id,
        ...ticket,
        status: 'VALID',
        sha256: 'mock-sha256',
        uploadedAt: new Date().toISOString(),
      };
      stored.set(id, view);
      return HttpResponse.json(view);
    }),
  ];
}
