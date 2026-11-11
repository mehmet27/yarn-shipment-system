import { DISCREPANCIES_PATH, ME_PATH } from '@iplik/api-client';
import { UiProvider } from '@iplik/ui';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
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
  it('açık uyuşmazlık sayısını rozet olarak gösterir', async () => {
    server.use(
      http.get(ME_PATH, () => HttpResponse.json(createMockUser('ADMIN'))),
      http.get(DISCREPANCIES_PATH, () =>
        HttpResponse.json({
          items: [],
          page: { number: 0, size: 5, totalElements: 7, totalPages: 2 },
        }),
      ),
    );
    renderBell();
    expect(await screen.findByText('7')).toBeInTheDocument();
  });

  it('uyuşmazlık görme izni yoksa zil çıkmaz', async () => {
    server.use(http.get(ME_PATH, () => HttpResponse.json(createMockUser('WAREHOUSE'))));
    renderBell();
    await new Promise((resolve) => setTimeout(resolve, 50));
    expect(screen.queryByRole('button', { name: 'Bildirimler' })).toBeNull();
  });
});
