import { DISCREPANCIES_PATH, ME_PATH, SHIPMENTS_PATH } from '@iplik/api-client';
import { UiProvider } from '@iplik/ui';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter } from 'react-router';
import { describe, expect, it } from 'vitest';
import { createMockUser } from '@/mocks/users';
import { server } from '@/mocks/server';
import { NotificationBell } from './NotificationBell';

function renderBell() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <UiProvider env="test">
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <NotificationBell />
        </MemoryRouter>
      </QueryClientProvider>
    </UiProvider>,
  );
}

describe('NotificationBell', () => {
  it('açık ve incelemedeki uyuşmazlıkların toplamını rozet olarak gösterir', async () => {
    server.use(
      http.get(ME_PATH, () => HttpResponse.json(createMockUser('MANAGER'))),
      http.get(DISCREPANCIES_PATH, ({ request }) =>
        HttpResponse.json({
          items: [],
          page: {
            number: 0,
            size: 5,
            totalElements: new URL(request.url).searchParams.get('status') === 'OPEN' ? 5 : 2,
            totalPages: 2,
          },
        }),
      ),
      http.get(SHIPMENTS_PATH, () =>
        HttpResponse.json({
          items: [],
          page: { number: 0, size: 20, totalElements: 0, totalPages: 0 },
        }),
      ),
    );
    renderBell();
    expect(await screen.findByText('7')).toBeInTheDocument();
  });

  it('kantar kararı bekleyen sevkiyatı rozete ekler ve menüde listeler', async () => {
    server.use(
      http.get(ME_PATH, () => HttpResponse.json(createMockUser('MANAGER'))),
      http.get(DISCREPANCIES_PATH, () =>
        HttpResponse.json({
          items: [],
          page: { number: 0, size: 5, totalElements: 0, totalPages: 0 },
        }),
      ),
      http.get(SHIPMENTS_PATH, () =>
        HttpResponse.json({
          items: [
            {
              shipmentId: 'shp-x',
              shipmentNo: 'SVK-2026-0099',
              status: 'WEIGHT_MISMATCH',
              subcontractorTitle: 'Örnek Tekstil A.Ş.',
              allowedActions: ['RESOLVE_WEIGHT_MISMATCH'],
            },
          ],
          page: { number: 0, size: 20, totalElements: 1, totalPages: 1 },
        }),
      ),
    );
    renderBell();
    expect(await screen.findByText('1')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Bildirimler' }));
    expect(await screen.findByText('SVK-2026-0099')).toBeInTheDocument();
    expect(screen.getByText('Kantar uyuşmazlığı: karar bekleyen sevkiyatlar')).toBeInTheDocument();
  });

  it('uyuşmazlık görme izni yoksa zil çıkmaz', async () => {
    server.use(http.get(ME_PATH, () => HttpResponse.json(createMockUser('WAREHOUSE'))));
    renderBell();
    await new Promise((resolve) => setTimeout(resolve, 50));
    expect(screen.queryByRole('button', { name: 'Bildirimler' })).toBeNull();
  });
});
