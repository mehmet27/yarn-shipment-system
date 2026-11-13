import { changePasswordUrl } from '@iplik/api-client';
import { UiProvider } from '@iplik/ui';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ME_PATH } from '@iplik/api-client';
import { createMockUser } from '@/mocks/users';
import { server } from '@/mocks/server';
import { UserMenu } from './UserMenu';

function renderMenu() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <UiProvider env="test">
      <QueryClientProvider client={queryClient}>
        <UserMenu />
      </QueryClientProvider>
    </UiProvider>,
  );
}

describe('UserMenu', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('kişinin adını, rolünü ve doğrulama durumunu gösterir', async () => {
    const user = createMockUser('ADMIN');
    server.use(http.get(ME_PATH, () => HttpResponse.json(user)));
    renderMenu();

    await userEvent.click(await screen.findByRole('button', { name: user.fullName }));

    expect(await screen.findByText('Yönetici')).toBeInTheDocument();
    expect(screen.getByText('✓ İki adımlı doğrulama açık')).toBeInTheDocument();
  });

  it('şifre değiştirme kendi sayfasına gider', async () => {
    const user = createMockUser('ADMIN');
    server.use(http.get(ME_PATH, () => HttpResponse.json(user)));
    renderMenu();

    await userEvent.click(await screen.findByRole('button', { name: user.fullName }));
    const assign = vi.fn();
    vi.stubGlobal('location', { assign });
    await userEvent.click(await screen.findByRole('menuitem', { name: 'Şifremi değiştir' }));

    expect(assign).toHaveBeenCalledWith(changePasswordUrl());
  });
});
