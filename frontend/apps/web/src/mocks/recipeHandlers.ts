import type { RecipeLineView } from '@iplik/api-client';
import { SHIPMENTS_PATH } from '@iplik/api-client';
import { compareWeighing, sumKg } from '@iplik/units';
import { http, HttpResponse } from 'msw';
import { findMockShipment, updateMockRecipe } from './mockStore';

const problem = (status: number, code: string, title: string) =>
  HttpResponse.json({ status, code, title }, { status });

type Body = {
  reason?: unknown;
  lines?: { netKg?: unknown; yarnTypeId?: unknown; yarnTypeText?: unknown }[];
};

function toViews(input: NonNullable<Body['lines']>): RecipeLineView[] {
  return input.map((line, index) => ({
    lineNo: index + 1,
    netKg: String(line.netKg),
    ...(typeof line.yarnTypeId === 'string' ? { yarnTypeId: line.yarnTypeId } : {}),
    ...(typeof line.yarnTypeText === 'string' ? { yarnTypeText: line.yarnTypeText } : {}),
  }));
}

const fitsWeighing = (total: string, measuredKg: string) =>
  compareWeighing(total, measuredKg, '0.000')?.exceedsTolerance === false;

const isValidRequest = (
  reason: string,
  count: number,
  shipment: { lines?: unknown[] | undefined },
) => reason !== '' && count >= (shipment.lines?.length ?? 0);

const mismatchProblem = (total: string, measuredKg: string) =>
  HttpResponse.json(
    {
      status: 409,
      code: 'RECIPE_WEIGHT_MISMATCH',
      title: 'Reçete tartıyı tutmuyor',
      recipeKg: total,
      measuredKg,
      toleranceKg: '0.000',
    },
    { status: 409 },
  );

/**
 * Reçete düzeltme: toplam tartılan net'i tutmuyorsa 409 (hiçbir şey kaydedilmez); tutuyorsa sevkiyat yola çıkar.
 * Mock'ta kantar toleransı 0'dır.
 */
export const updateRecipeHandler = http.put(
  `${SHIPMENTS_PATH}/:shipmentId/recipe`,
  async ({ params, request }) => {
    const shipment = findMockShipment(params.shipmentId);
    if (shipment === undefined) return problem(404, 'NOT_FOUND', 'Bulunamadı');
    const body = (await request.json()) as Body;
    const reason = typeof body.reason === 'string' ? body.reason.trim() : '';
    const lines = toViews(body.lines ?? []);
    if (!isValidRequest(reason, lines.length, shipment)) {
      return problem(400, 'INVALID_WEIGHT_DECISION', 'Geçersiz istek');
    }
    const measuredKg =
      shipment.status === 'WEIGHT_MISMATCH' ? shipment.weightCheck?.measuredKg : undefined;
    if (measuredKg === undefined) return problem(409, 'INVALID_SHIPMENT_STATUS', 'Geçersiz durum');
    const total = sumKg(lines.map((line) => line.netKg));
    if (!fitsWeighing(total, measuredKg)) return mismatchProblem(total, measuredKg);
    updateMockRecipe(shipment.shipmentId, lines, reason);
    return new HttpResponse(null, { status: 204 });
  },
);
