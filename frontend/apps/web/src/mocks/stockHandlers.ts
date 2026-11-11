import type { CurrentUser } from '@iplik/api-client';
import type { CentralStockMovement, CentralStockRow } from '@iplik/api-client/central-stock';
import { CENTRAL_STOCK_PATH } from '@iplik/api-client/central-stock';
import { isKilogram, sumKg } from '@iplik/units';
import { http, HttpResponse } from 'msw';

const problem = (status: number, code: string) =>
  HttpResponse.json({ status, code, title: code }, { status });
const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null;
const text = (value: unknown): value is string => typeof value === 'string' && value.trim() !== '';
const positiveKg = (value: unknown): value is string =>
  typeof value === 'string' && isKilogram(value) && Number(value) > 0;

/** Sahte API'nin merkez iplik stoku uçları; durum her `createStockHandlers` çağrısında sıfırdan başlar. */
export function createStockHandlers(user: CurrentUser) {
  const has = (permission: (typeof user.permissions)[number]) =>
    user.permissions.includes(permission);
  const movements: CentralStockMovement[] = [];
  let counter = 0;
  const add = (yarnType: string, kind: CentralStockMovement['kind'], netKg: string) => {
    counter += 1;
    const movement: CentralStockMovement = {
      id: `stock-move-${String(counter)}`,
      yarnType,
      kind,
      netKg,
      occurredAt: '2026-11-13T09:00:00Z',
    };
    movements.unshift(movement);
    return movement;
  };
  const rows = (): CentralStockRow[] =>
    [...new Set(movements.map((m) => m.yarnType))].sort().map((yarnType) => ({
      yarnType,
      stockKg: sumKg(movements.filter((m) => m.yarnType === yarnType).map((m) => m.netKg)),
    }));
  /** Her iplik payını kumaş / (1 - fire) kadar düşer; satır geçersizse undefined. */
  const consume = (yarns: unknown[], fire: number) => {
    const consumed: CentralStockMovement[] = [];
    for (const share of yarns) {
      if (!isRecord(share) || !text(share.yarnType) || !positiveKg(share.netKg)) return undefined;
      const kg = (Number(share.netKg) / (1 - fire)).toFixed(3);
      consumed.push(add(share.yarnType, 'PRODUCTION', `-${kg}`));
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
        movements,
      });
    }),
    http.post(`${CENTRAL_STOCK_PATH}/yarn-receipts`, async ({ request }) => {
      if (!has('CENTRAL_STOCK_RECORD')) return problem(403, 'ACCESS_DENIED');
      const json: unknown = await request.json();
      if (!isRecord(json) || !text(json.yarnType) || !positiveKg(json.netKg)) {
        return problem(400, 'INVALID_LEDGER_INPUT');
      }
      return HttpResponse.json(add(json.yarnType.trim(), 'YARN_RECEIPT', json.netKg), {
        status: 201,
      });
    }),
    http.post(`${CENTRAL_STOCK_PATH}/productions`, async ({ request }) => {
      if (!has('CENTRAL_PRODUCTION_RECORD')) return problem(403, 'ACCESS_DENIED');
      const json: unknown = await request.json();
      if (!isRecord(json) || !text(json.fabricType) || !Array.isArray(json.yarns)) {
        return problem(400, 'INVALID_LEDGER_INPUT');
      }
      const consumed = consume(json.yarns as unknown[], Number(json.fireRate));
      if (consumed === undefined) return problem(400, 'INVALID_LEDGER_INPUT');
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
