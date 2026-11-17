import { ME_PATH, type Role } from '@iplik/api-client';
import { UiProvider } from '@iplik/ui';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { MemoryRouter } from 'react-router';
import { describe, expect, it } from 'vitest';
import { server } from '@/mocks/server';
import { createMockUser } from '@/mocks/users';
import { PanelNav } from './PanelNav';

function renderNav(role: Role) {
  server.use(http.get(ME_PATH, () => HttpResponse.json(createMockUser(role))));
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <UiProvider env="test">
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <PanelNav onNavigate={() => undefined} />
        </MemoryRouter>
      </QueryClientProvider>
    </UiProvider>,
  );
}

describe('PanelNav', () => {
  it('müdür günlük işleri ve ayarları iki başlık altında görür', async () => {
    renderNav('MANAGER');
    expect(await screen.findByText('Günlük işler')).toBeInTheDocument();
    expect(screen.getByText('Tanımlar ve ayarlar')).toBeInTheDocument();
  });

  it('depocuda tek liste kalır, başlık yazmaz', async () => {
    renderNav('WAREHOUSE');
    expect(await screen.findByRole('link', { name: 'Sevkiyatlar' })).toBeInTheDocument();
    expect(screen.queryByText('Günlük işler')).not.toBeInTheDocument();
    expect(screen.queryByText('Tanımlar ve ayarlar')).not.toBeInTheDocument();
  });
});
