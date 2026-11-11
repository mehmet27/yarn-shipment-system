import type { CurrentUser } from '@iplik/api-client';
import type {
  FabricReceiptRequest,
  FireSettlementRow,
  YarnReturnRequest,
} from '@iplik/api-client/settlements';
import { SHIPMENTS_PATH } from '@iplik/api-client';
import { BALANCES_PATH, FIRE_SETTLEMENTS_PATH, FIRE_STATUSES } from '@iplik/api-client/settlements';
import { isKilogram, sumKg } from '@iplik/units';
import { http, HttpResponse } from 'msw';
import { findMockShipment, listMockShipments } from './mockStore';
import {
  addMockReceipt,
  addMockReturn,
  closeMockFile,
  getMockBalanceRows,
  getMockSettlement,
  isMockClosed,
  listMockMovements,
  listMockReceipts,
  listMockReturns,
  markMockInvoiced,
  setMockRate,
} from './settlements';

const DEFAULT_SIZE = 20;
const MAX_RATE = 0.2;
const problem = (status: number, code: string) =>
  HttpResponse.json({ status, code, title: code }, { status });
const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null;
const text = (value: unknown): value is string => typeof value === 'string' && value.trim() !== '';
const positiveKg = (value: unknown): value is string =>
  typeof value === 'string' && isKilogram(value) && Number(value) > 0;

function readReceipt(json: unknown): FabricReceiptRequest | undefined {
  if (!isRecord(json) || !text(json.fabricType) || !text(json.waybillNo)) return undefined;
  if (!positiveKg(json.netKg)) return undefined;
  const yarns = json.yarns;
  const base = {
    fabricType: json.fabricType,
    netKg: json.netKg,
    waybillNo: json.waybillNo,
    ...(typeof json.rollCount === 'number' ? { rollCount: json.rollCount } : {}),
  };
  if (yarns === undefined) return base;
  if (!Array.isArray(yarns)) return undefined;
  const shares = yarns.filter(
    (y): y is { yarnType: string; netKg: string } =>
      isRecord(y) && text(y.yarnType) && positiveKg(y.netKg),
  );
  return shares.length === yarns.length ? { ...base, yarns: shares } : undefined;
}

function readReturn(json: unknown): YarnReturnRequest | undefined {
  if (!isRecord(json) || !text(json.yarnType) || !positiveKg(json.netKg)) return undefined;
  const { direction } = json;
  if (direction !== 'RETURN' && direction !== 'FROM_SUBCONTRACTOR') return undefined;
  return {
    direction,
    yarnType: json.yarnType,
    netKg: json.netKg,
    ...(text(json.lotId) ? { lotId: json.lotId } : {}),
    ...(text(json.waybillNo) ? { waybillNo: json.waybillNo } : {}),
  };
}

/** Sahte API'nin fire dosyası, kumaş, iplik dönüşü ve bakiye uçları; izinler backend matrisini taklit eder. */
export function createSettlementHandlers(user: CurrentUser) {
  const has = (permission: (typeof user.permissions)[number]) =>
    user.permissions.includes(permission);
  /** Yalnız tamamlanmış sevkiyatın fire dosyası vardır. */
  const completed = (shipmentId: unknown) => {
    const shipment = findMockShipment(shipmentId);
    if (shipment === undefined) return problem(404, 'SHIPMENT_NOT_FOUND');
    return shipment.status === 'COMPLETED' ? undefined : problem(409, 'SHIPMENT_NOT_COMPLETED');
  };
  const url = (path: string) => `${SHIPMENTS_PATH}/:shipmentId/${path}`;
  return [
    http.get(BALANCES_PATH, ({ request }) => {
      if (!has('BALANCE_VIEW')) return problem(403, 'ACCESS_DENIED');
      const id = new URL(request.url).searchParams.get('subcontractorId');
      const items = getMockBalanceRows().filter((r) => id === null || r.subcontractorId === id);
      return HttpResponse.json({ items, totalKg: sumKg(items.map((r) => r.balanceKg)) });
    }),
    http.get(`${BALANCES_PATH}/movements`, ({ request }) => {
      if (!has('BALANCE_VIEW')) return problem(403, 'ACCESS_DENIED');
      const params = new URL(request.url).searchParams;
      const page = Number(params.get('page') ?? '0');
      const size = Number(params.get('size') ?? String(DEFAULT_SIZE));
      const matching = listMockMovements().filter(
        (m) =>
          (params.get('subcontractorId') ?? m.subcontractorId) === m.subcontractorId &&
          (params.get('yarnType') ?? m.yarnType) === m.yarnType,
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
    http.get(
      url('fabric-receipts'),
      ({ params }) =>
        completed(params.shipmentId) ??
        HttpResponse.json(listMockReceipts(String(params.shipmentId))),
    ),
    http.post(url('fabric-receipts'), async ({ params, request }) => {
      const id = String(params.shipmentId);
      if (!has('FABRIC_RECEIPT_RECORD')) return problem(403, 'ACCESS_DENIED');
      const failure = completed(id);
      if (failure !== undefined) return failure;
      const body = readReceipt(await request.json());
      if (body === undefined) return problem(400, 'VALIDATION_FAILED');
      if (isMockClosed(id)) return problem(409, 'FIRE_FILE_CLOSED');
      if (
        body.yarns !== undefined &&
        sumKg(body.yarns.map((y) => y.netKg)) !== sumKg([body.netKg])
      ) {
        return problem(400, 'YARN_DISTRIBUTION_MISMATCH');
      }
      return HttpResponse.json(addMockReceipt(id, body, user.userId), { status: 201 });
    }),
    http.get(
      url('yarn-returns'),
      ({ params }) =>
        completed(params.shipmentId) ??
        HttpResponse.json(listMockReturns(String(params.shipmentId))),
    ),
    http.post(url('yarn-returns'), async ({ params, request }) => {
      const id = String(params.shipmentId);
      if (!has('YARN_RETURN_RECORD')) return problem(403, 'ACCESS_DENIED');
      const failure = completed(id);
      if (failure !== undefined) return failure;
      const body = readReturn(await request.json());
      if (body === undefined) return problem(400, 'VALIDATION_FAILED');
      if (isMockClosed(id)) return problem(409, 'FIRE_FILE_CLOSED');
      return HttpResponse.json(addMockReturn(id, body, user.userId), { status: 201 });
    }),
    http.get(url('fire-settlement'), ({ params }) => {
      if (!has('BALANCE_VIEW') && !has('FIRE_RATE_EDIT') && !has('INVOICE_MARK')) {
        return problem(403, 'ACCESS_DENIED');
      }
      return (
        completed(params.shipmentId) ??
        HttpResponse.json(getMockSettlement(String(params.shipmentId)))
      );
    }),
    http.patch(url('fire-rate'), async ({ params, request }) => {
      const id = String(params.shipmentId);
      if (!has('FIRE_RATE_EDIT')) return problem(403, 'ACCESS_DENIED');
      const failure = completed(id);
      if (failure !== undefined) return failure;
      const json: unknown = await request.json();
      const fireRate = isRecord(json) ? json.fireRate : undefined;
      const reason = isRecord(json) ? json.reason : undefined;
      const isValidRate =
        typeof fireRate === 'string' &&
        /^0(\.\d{1,4})?$/.test(fireRate) &&
        Number(fireRate) <= MAX_RATE;
      if (!isValidRate || !text(reason)) return problem(400, 'VALIDATION_FAILED');
      if (isMockClosed(id)) return problem(409, 'FIRE_FILE_CLOSED');
      setMockRate(id, fireRate);
      return HttpResponse.json(getMockSettlement(id));
    }),
    http.post(url('fire-settlement/invoice'), async ({ params, request }) => {
      const id = String(params.shipmentId);
      if (!has('INVOICE_MARK')) return problem(403, 'ACCESS_DENIED');
      const failure = completed(id);
      if (failure !== undefined) return failure;
      const json: unknown = await request.json();
      if (!isRecord(json) || !text(json.invoiceNo) || !text(json.invoicedOn)) {
        return problem(400, 'VALIDATION_FAILED');
      }
      if (isMockClosed(id) || Number(getMockSettlement(id).faultyKg) <= 0) {
        return problem(409, 'FIRE_INVOICE_NOT_APPLICABLE');
      }
      markMockInvoiced(id, {
        invoiceNo: json.invoiceNo.trim(),
        invoicedAt: json.invoicedOn,
        ...(text(json.note) ? { invoiceNote: json.note.trim() } : {}),
      });
      return HttpResponse.json(getMockSettlement(id));
    }),
    http.post(url('fire-settlement/close'), ({ params }) => {
      const id = String(params.shipmentId);
      if (!has('INVOICE_MARK')) return problem(403, 'ACCESS_DENIED');
      const failure = completed(id);
      if (failure !== undefined) return failure;
      if (isMockClosed(id) || Number(getMockSettlement(id).faultyKg) > 0) {
        return problem(409, 'FIRE_CLOSE_NOT_APPLICABLE');
      }
      closeMockFile(id);
      return HttpResponse.json(getMockSettlement(id));
    }),
    http.get(FIRE_SETTLEMENTS_PATH, ({ request }) => {
      if (!has('BALANCE_VIEW') && !has('INVOICE_MARK')) return problem(403, 'ACCESS_DENIED');
      const params = new URL(request.url).searchParams;
      const status = FIRE_STATUSES.find((s) => s === params.get('status'));
      const page = Number(params.get('page') ?? '0');
      const size = Number(params.get('size') ?? String(DEFAULT_SIZE));
      const rows: FireSettlementRow[] = listMockShipments()
        .filter((s) => s.status === 'COMPLETED')
        .filter((s) => (params.get('subcontractorId') ?? s.subcontractorId) === s.subcontractorId)
        .map((s) => ({ shipment: s, settlement: getMockSettlement(s.shipmentId) }))
        .filter(({ settlement }) => status === undefined || settlement.status === status)
        .map(({ shipment, settlement }) => ({
          shipmentId: shipment.shipmentId,
          shipmentNo: shipment.shipmentNo,
          subcontractorTitle: shipment.subcontractorTitle,
          status: settlement.status,
          sentKg: settlement.sentKg,
          fabricKg: settlement.fabricKg,
          expectedFabricKg: settlement.expectedFabricKg,
          faultyKg: settlement.faultyKg,
          actualFirePercent: settlement.actualFirePercent,
        }));
      return HttpResponse.json({
        items: rows.slice(page * size, (page + 1) * size),
        page: {
          number: page,
          size,
          totalElements: rows.length,
          totalPages: Math.ceil(rows.length / size),
        },
      });
    }),
  ];
}
