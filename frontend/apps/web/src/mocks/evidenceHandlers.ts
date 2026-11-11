import type { EvidenceType, Permission } from '@iplik/api-client';
import { EvidenceType as EVIDENCE_TYPES } from '@iplik/api-client';
import { http, HttpResponse } from 'msw';
import { UPLOAD_PERMISSION_BY_TYPE } from '@/modules/evidence/uploadTypes';

const EVIDENCE_PATH = '/api/v1/evidence/uploads';
const STORAGE_URL = 'https://mock-storage.local/evidence';
const MAX_BYTES = 20 * 1024 * 1024;
const ALLOWED_MIME = ['image/jpeg', 'image/png', 'image/heic', 'application/pdf'];
const TICKET_TTL_MS = 600_000;
/** Bu boyutun altındaki yüklemeler "okunamayan fiş" sayılır; testler iki yolu da seçebilir. */
export const UNREADABLE_BELOW_BYTES = 1000;

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
const UNVERIFIED_REASONS = ['PRINTER_BROKEN', 'TICKET_DAMAGED', 'NO_TICKET', 'OTHER'] as const;

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
    http.post('/api/v1/evidence/:evidenceId/ticket-reading', ({ params }) => {
      const id = String(params.evidenceId);
      const ticket = pending.get(id);
      if (ticket === undefined) return problem(404, 'UPLOAD_NOT_FOUND');
      const base = {
        evidenceId: id,
        shipmentId: ticket.shipmentId,
        evidenceType: ticket.evidenceType,
        readAt: new Date().toISOString(),
      };
      if (ticket.sizeBytes < UNREADABLE_BELOW_BYTES) {
        return HttpResponse.json({ ...base, status: 'UNREADABLE', problems: ['WEIGHT_NOT_READ'] });
      }
      return HttpResponse.json({
        ...base,
        status: 'READ',
        grossKg: 12500,
        tareKg: 2500,
        netKg: 10000,
        problems: [],
      });
    }),
    http.post(
      '/api/v1/evidence/:evidenceId/ticket-reading/unverified',
      async ({ params, request }) => {
        const id = String(params.evidenceId);
        const ticket = pending.get(id);
        if (ticket === undefined) return problem(404, 'UPLOAD_NOT_FOUND');
        const json: unknown = await request.json();
        const reason = UNVERIFIED_REASONS.find((value) => isRecord(json) && json.reason === value);
        if (reason === undefined) return problem(400, 'VALIDATION_FAILED');
        if (ticket.sizeBytes >= UNREADABLE_BELOW_BYTES) {
          return problem(409, 'TICKET_NOT_UNREADABLE');
        }
        return HttpResponse.json({
          evidenceId: id,
          shipmentId: ticket.shipmentId,
          evidenceType: ticket.evidenceType,
          reason,
          declaredAt: new Date().toISOString(),
        });
      },
    ),
    http.get('/api/v1/evidence/:evidenceId/download-url', () =>
      HttpResponse.json({
        url: PHOTO_DATA_URL,
        expiresAt: new Date(Date.now() + TICKET_TTL_MS).toISOString(),
      }),
    ),
    http.post(`${EVIDENCE_PATH}/:evidenceId/confirm`, ({ params }) => {
      const id = String(params.evidenceId);
      const ticket = pending.get(id);
      if (ticket === undefined) return problem(404, 'UPLOAD_NOT_FOUND');
      if (!uploaded.has(id)) return problem(409, 'UPLOAD_NOT_COMPLETED');
      return HttpResponse.json({
        id,
        ...ticket,
        sha256: 'mock-sha256',
        uploadedAt: new Date().toISOString(),
      });
    }),
  ];
}
