import { isAxiosError } from 'axios';

/** RFC 9457 Problem Details (Java ERR-07). Kararlar `code` alanına göre verilir (FE-API-05). */
export type Problem = {
  status: number;
  code: string;
  title: string;
  detail?: string;
  requestId?: string;
  errors?: readonly FieldError[];
  /** Yalnızca `CENTRAL_STOCK_SHORTFALL` ve `SOURCE_YARN_SHORTAGE`: stoğu yetmeyen iplikler ve eksik kilo. */
  shortfalls?: readonly StockShortfall[];
  /** Yalnızca `RECIPE_WEIGHT_MISMATCH`: reçete toplamı, tartılan net ve kantar toleransı (kg metni). */
  recipeKg?: string;
  /** Yalnızca `BAG_WEIGHT_MISMATCH`: çuvalların toplamı; `measuredKg` araç net kilosu, `toleranceKg` kantar toleransıdır. */
  bagKg?: string;
  measuredKg?: string;
  toleranceKg?: string;
  /** Yalnızca `BAG_COUNT_MISMATCH`: tartıya yazılan çuval sayısı ve sevkiyata eklenen çuval sayısı. */
  /** Yalnızca `INVALID_FIRE_SETTLEMENT_ACTION`: işlemin reddedilme nedeni (ör. `NOTHING_TO_INVOICE`). */
  reason?: string;
  enteredCount?: number;
  bagCount?: number;
};

export type StockShortfall = { yarnType: string; missingKg: string };

export type FieldError = { field: string; message: string };

export type ProblemKind =
  | 'unauthenticated'
  | 'forbidden'
  | 'notFound'
  | 'conflict'
  | 'validation'
  | 'unavailable'
  | 'unexpected';

const NETWORK_ERROR: Problem = { status: 0, code: 'NETWORK_ERROR', title: 'Network error' };

function isProblemBody(body: unknown): body is Problem {
  return typeof body === 'object' && body !== null && 'code' in body && 'status' in body;
}

export function toProblem(error: unknown): Problem {
  if (!isAxiosError(error) || error.response === undefined) {
    return NETWORK_ERROR;
  }
  const body: unknown = error.response.data;
  if (isProblemBody(body)) {
    return body;
  }
  return { status: error.response.status, code: 'UNKNOWN', title: error.message };
}

/** FE-API-06: HTTP durumunun ekrandaki karşılığı tek yerde. */
export function classifyProblem(problem: Problem): ProblemKind {
  const kindsByStatus: Readonly<Record<number, ProblemKind>> = {
    400: 'validation',
    401: 'unauthenticated',
    403: 'forbidden',
    404: 'notFound',
    409: 'conflict',
    422: 'validation',
    503: 'unavailable',
  };
  return kindsByStatus[problem.status] ?? 'unexpected';
}
