import { getCurrentUser, getShipment, ME_PATH, SHIPMENTS_PATH } from '@iplik/api-client';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { createMockUser } from '@/mocks/users';
import { server } from '@/mocks/server';
import { CURRENT_USER_QUERY_KEY } from '@/modules/auth';
import { createQueryClient } from './queryClient';

const expired = () =>
  HttpResponse.json(
    { status: 401, code: 'AUTHENTICATION_REQUIRED', title: 'Oturum' },
    { status: 401 },
  );

describe('createQueryClient', () => {
  it('oturum süresi dolunca kullanıcı sorgusunu eskitir', async () => {
    let meCalls = 0;
    server.use(
      http.get(ME_PATH, () => {
        meCalls += 1;
        return meCalls === 1 ? HttpResponse.json(createMockUser('DRIVER')) : expired();
      }),
      http.get(`${SHIPMENTS_PATH}/x`, expired),
    );
    const client = createQueryClient();
    await client.query({ queryKey: CURRENT_USER_QUERY_KEY, queryFn: getCurrentUser });
    await client.query({ queryKey: ['x'], queryFn: () => getShipment('x') }).catch(() => undefined);
    expect(client.getQueryState(CURRENT_USER_QUERY_KEY)?.isInvalidated).toBe(true);
    expect(meCalls).toBe(1);
  });
});
