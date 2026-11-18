import { HttpResponse } from 'msw';
import { sumKg } from '@iplik/units';
import { assignedMockBagWeights } from './mockStore';

/** Sayı sonra kilo: eklenen çuvallar tartıyı tutmuyorsa 409 BAG_COUNT_MISMATCH / BAG_WEIGHT_MISMATCH. */
export function bagMismatchResponse(
  shipmentId: string,
  weighing: { netKg: string; bagCount: number },
): Response | undefined {
  const weights = assignedMockBagWeights(shipmentId);
  if (weights.length !== weighing.bagCount) {
    return HttpResponse.json(
      {
        status: 409,
        code: 'BAG_COUNT_MISMATCH',
        title: 'Çuval sayısı tutmuyor',
        enteredCount: weighing.bagCount,
        bagCount: weights.length,
      },
      { status: 409 },
    );
  }
  const netKg = weighing.netKg;
  const bagKg = sumKg(weights);
  if (bagKg === netKg) return undefined;
  return HttpResponse.json(
    {
      status: 409,
      code: 'BAG_WEIGHT_MISMATCH',
      title: 'Çuval toplamı araç net kilosunu tutmuyor',
      bagKg,
      measuredKg: netKg,
      toleranceKg: '0.000',
    },
    { status: 409 },
  );
}
