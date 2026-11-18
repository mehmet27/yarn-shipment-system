import type { CurrentUser } from '@iplik/api-client';
import type {
  CentralStockMovement,
  CentralStockRow,
  SupplierReceipt,
} from '@iplik/api-client/central-stock';
import { CENTRAL_STOCK_PATH } from '@iplik/api-client/central-stock';
import { isKilogram, sumKg } from '@iplik/units';
import { http, HttpResponse } from 'msw';
import { listMasterRecords } from './masterData';

const problem = (status: number, code: string) =>
  HttpResponse.json({ status, code, title: code }, { status });
const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null;
const text = (value: unknown): value is string => typeof value === 'string' && value.trim() !== '';
const positiveKg = (value: unknown): value is string =>
  typeof value === 'string' && isKilogram(value) && Number(value) > 0;

/** Tanımlar'daki aktif iplik; pasif ya da olmayan id'yi sunucu reddeder. */
const activeYarn = (productId: unknown) =>
  listMasterRecords('product').find((p) => p.id === productId && p.isActive);

type ReceiptInput = {
  product: { id: string; label: string };
  netKg: string;
  receipt: NonNullable<CentralStockMovement['receipt']>;
  hasGrade: boolean;
};

/** İplik girişi gövdesini okur; iplik ya da tedarikçi aktif değilse ya da kilo geçersizse undefined. */
function readReceipt(json: unknown): ReceiptInput | undefined {
  if (!isRecord(json)) return undefined;
  const product = activeYarn(json.productId);
  const supplier = listMasterRecords('yarnSupplier').find(
    (s) => s.id === json.supplierId && s.isActive,
  );
  if (product === undefined || supplier === undefined || !positiveKg(json.netKg)) return undefined;
  return {
    product,
    netKg: json.netKg,
    hasGrade: text(json.qualityGradeId),
    receipt: {
      supplierName: supplier.label,
      ...(text(json.lotNo) ? { lotNo: json.lotNo } : {}),
      ...(text(json.waybillNo) ? { waybillNo: json.waybillNo } : {}),
    },
  };
}

/** Sahte API'nin merkez iplik stoku uçları; durum her `createStockHandlers` çağrısında sıfırdan başlar. */
export function createStockHandlers(user: CurrentUser) {
  const has = (permission: (typeof user.permissions)[number]) =>
    user.permissions.includes(permission);
  const movements: CentralStockMovement[] = [];
  let counter = 0;
  const producedKg: string[] = [];
  const add = (
    product: { id: string; label: string },
    kind: CentralStockMovement['kind'],
    netKg: string,
  ) => {
    counter += 1;
    const movement: CentralStockMovement = {
      id: `stock-move-${String(counter)}`,
      productId: product.id,
      yarnType: product.label,
      kind,
      netKg,
      occurredAt: '2026-11-13T09:00:00Z',
    };
    movements.unshift(movement);
    return movement;
  };
  const rows = (): CentralStockRow[] =>
    [...new Set(movements.map((m) => m.productId))].flatMap((productId) => {
      const own = movements.filter((m) => m.productId === productId);
      return own[0] === undefined || productId === undefined
        ? []
        : [{ productId, yarnType: own[0].yarnType, stockKg: sumKg(own.map((m) => m.netKg)) }];
    });
  const supplierRows = (): SupplierReceipt[] => {
    const received = movements.filter((m) => m.kind === 'YARN_RECEIPT' && m.receipt !== undefined);
    return [...new Set(received.map((m) => `${m.receipt?.supplierName ?? ''}/${m.yarnType}`))].map(
      (key) => {
        const own = received.filter(
          (m) => `${m.receipt?.supplierName ?? ''}/${m.yarnType}` === key,
        );
        return {
          supplierName: own[0]?.receipt?.supplierName ?? '',
          ...(own[0]?.productId === undefined ? {} : { productId: own[0].productId }),
          yarnType: own[0]?.yarnType ?? '',
          receivedKg: sumKg(own.map((m) => m.netKg)),
        };
      },
    );
  };
  /** Her iplik payını kumaş / (1 - fire) kadar düşer; satır geçersizse undefined. */
  const consume = (yarns: unknown[], fire: number) => {
    const consumed: CentralStockMovement[] = [];
    for (const share of yarns) {
      if (!isRecord(share) || !positiveKg(share.netKg)) return undefined;
      const product = activeYarn(share.productId);
      if (product === undefined) return undefined;
      const kg = (Number(share.netKg) / (1 - fire)).toFixed(3);
      consumed.push(add(product, 'PRODUCTION', `-${kg}`));
    }
    return consumed;
  };
  return [
    http.get(CENTRAL_STOCK_PATH, () => {
      if (!has('CENTRAL_STOCK_VIEW')) return problem(403, 'ACCESS_DENIED');
      const current = rows();
      return HttpResponse.json({
        rows: current,
        totalKg: sumKg(current.map((r) => r.stockKg)),
        producedFabricKg: sumKg(producedKg),
        flow: {
          receivedKg: sumKg(movements.filter((m) => m.kind === 'YARN_RECEIPT').map((m) => m.netKg)),
          directTransferKg: '0.000',
          atSubcontractorsKg: sumKg(
            movements
              .filter((m) => m.kind === 'TO_SUBCONTRACTOR')
              .map((m) => m.netKg.replace('-', '')),
          ),
          usedInCentralProductionKg: sumKg(
            movements.filter((m) => m.kind === 'PRODUCTION').map((m) => m.netKg.replace('-', '')),
          ),
        },
        movements,
        suppliers: supplierRows(),
      });
    }),
    http.post(`${CENTRAL_STOCK_PATH}/yarn-receipts`, async ({ request }) => {
      if (!has('CENTRAL_STOCK_RECORD')) return problem(403, 'ACCESS_DENIED');
      const input = readReceipt(await request.json());
      if (input === undefined) return problem(400, 'INVALID_LEDGER_INPUT');
      if (input.receipt.lotNo !== undefined && !input.hasGrade) {
        return problem(409, 'YARN_LOT_GRADE_REQUIRED');
      }
      const movement = add(input.product, 'YARN_RECEIPT', input.netKg);
      movement.receipt = input.receipt;
      return HttpResponse.json(movement, { status: 201 });
    }),
    http.post(`${CENTRAL_STOCK_PATH}/productions`, async ({ request }) => {
      if (!has('CENTRAL_PRODUCTION_RECORD')) return problem(403, 'ACCESS_DENIED');
      const json: unknown = await request.json();
      if (!isRecord(json) || !text(json.fabricType) || !Array.isArray(json.yarns)) {
        return problem(400, 'INVALID_LEDGER_INPUT');
      }
      const consumed = consume(json.yarns as unknown[], Number(json.fireRate));
      if (consumed === undefined) return problem(400, 'INVALID_LEDGER_INPUT');
      producedKg.push(sumKg((json.yarns as { netKg: string }[]).map((y) => y.netKg)));
      return HttpResponse.json(
        {
          id: 'central-production-1',
          fabricType: json.fabricType,
          netKg: sumKg((json.yarns as { netKg: string }[]).map((y) => y.netKg)),
          fireRate: String(json.fireRate),
          consumptions: consumed,
        },
        { status: 201 },
      );
    }),
  ];
}
