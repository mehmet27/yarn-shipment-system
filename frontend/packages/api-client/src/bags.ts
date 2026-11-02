import { isAxiosError } from 'axios';
import type { PrintLabelsRequest } from './generated/bagService';
import { httpClient } from './httpClient';

export {
  BagStatus,
  createBags,
  listLockedLots,
  listLotBags,
  PackageType,
} from './generated/bagService';
export type {
  BagView,
  CreateBagsRequest,
  CreateBagsResponse,
  LabeledBagView,
  LockedLotView,
  LotBagsResponse,
  PrintLabelsRequest,
} from './generated/bagService';

export const BAGS_PATH = '/api/v1/bags';

/**
 * Etiket PDF'ini ister. Üretilen `printLabels` yerine elle yazılır: ikili cevap `arraybuffer` olarak
 * alınır, hata gövdesi (Problem) de ikili gelir ve `toProblem` kodu okuyabilsin diye JSON'a çevrilir.
 */
export async function printBagLabels(request: PrintLabelsRequest): Promise<Blob> {
  try {
    const response = await httpClient.post<ArrayBuffer>(`${BAGS_PATH}/labels`, request, {
      responseType: 'arraybuffer',
    });
    return new Blob([response.data], { type: 'application/pdf' });
  } catch (error) {
    if (isAxiosError(error) && error.response?.data instanceof ArrayBuffer) {
      error.response.data = parseJson(new TextDecoder().decode(error.response.data));
    }
    throw error;
  }
}

function parseJson(text: string): unknown {
  try {
    const parsed: unknown = JSON.parse(text);
    return parsed;
  } catch {
    return text;
  }
}
