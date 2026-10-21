import { AxiosError, AxiosHeaders } from 'axios';
import { describe, expect, it } from 'vitest';
import { classifyProblem, toProblem } from './problem';

function axiosErrorWith(status: number, data: unknown): AxiosError {
  const config = { headers: new AxiosHeaders() };
  return new AxiosError('Request failed', 'ERR_BAD_RESPONSE', config, undefined, {
    status,
    statusText: '',
    headers: {},
    config,
    data,
  });
}

describe('toProblem', () => {
  it('Problem Details gövdesini olduğu gibi döner', () => {
    const body = { status: 409, code: 'SHIPMENT_LOCKED', title: 'Sevkiyat kilitli' };
    expect(toProblem(axiosErrorWith(409, body))).toEqual(body);
  });

  it('Problem Details olmayan yanıtı UNKNOWN koduyla sarar', () => {
    expect(toProblem(axiosErrorWith(500, '<html>'))).toMatchObject({
      status: 500,
      code: 'UNKNOWN',
    });
  });

  it('yanıt yoksa ağ hatası döner', () => {
    expect(toProblem(new Error('offline'))).toMatchObject({ code: 'NETWORK_ERROR' });
  });
});

describe('classifyProblem', () => {
  it.each([
    [401, 'unauthenticated'],
    [403, 'forbidden'],
    [404, 'notFound'],
    [409, 'conflict'],
    [422, 'validation'],
    [503, 'unavailable'],
    [500, 'unexpected'],
  ])('%i durumunu %s olarak sınıflar', (status, kind) => {
    expect(classifyProblem({ status, code: 'X', title: 'x' })).toBe(kind);
  });
});
