import { ME_PATH } from '@iplik/api-client';
import { UiProvider } from '@iplik/ui';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, Route, Routes } from 'react-router';
import { describe, expect, it } from 'vitest';
import { createMockUser } from '@/mocks/users';
import { server } from '@/mocks/server';
import { ChangePasswordPage } from './ChangePasswordPage';

function renderPage(passwordChangeRequired: boolean) {
  server.use(
    http.get(ME_PATH, () =>
      HttpResponse.json({ ...createMockUser('WAREHOUSE'), passwordChangeRequired }),
    ),
  );
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <UiProvider env="test">
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/sifre-degistir']}>
          <Routes>
            <Route path="/sifre-degistir" element={<ChangePasswordPage />} />
            <Route path="/" element={<p>ana sayfa</p>} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    </UiProvider>,
  );
}

async function fill(current: string, next: string, repeat: string) {
  await userEvent.type(screen.getByLabelText(/^Mevcut şifre/), current);
  await userEvent.type(screen.getByLabelText(/^Yeni şifre( \*)?$/), next);
  await userEvent.type(screen.getByLabelText(/^Yeni şifre \(tekrar\)/), repeat);
}

describe('ChangePasswordPage', () => {
  it('şifreyi değiştirince ana sayfaya döner', async () => {
    renderPage(false);
    await fill('eski-sifre', 'yeni-sifre-1', 'yeni-sifre-1');
    await userEvent.click(screen.getByRole('button', { name: 'Şifreyi değiştir' }));
    expect(await screen.findByText('ana sayfa')).toBeInTheDocument();
  });

  it('yeni şifreler aynı değilse uyarır ve göndermez', async () => {
    renderPage(false);
    await fill('eski-sifre', 'yeni-sifre-1', 'yeni-sifre-2');
    expect(screen.getByText('Yeni şifreler aynı değil.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Şifreyi değiştir' })).toBeDisabled();
  });

  it('mevcut şifre yanlışsa nedenini söyler', async () => {
    server.use(
      http.put('/api/v1/auth/password', () =>
        HttpResponse.json({ code: 'INVALID_CURRENT_PASSWORD', status: 400 }, { status: 400 }),
      ),
    );
    renderPage(false);
    await fill('yanlis', 'yeni-sifre-1', 'yeni-sifre-1');
    await userEvent.click(screen.getByRole('button', { name: 'Şifreyi değiştir' }));
    expect(await screen.findByText('Mevcut şifre hatalı.')).toBeInTheDocument();
  });

  it('geçici şifreyle girende Vazgeç gösterilmez', async () => {
    renderPage(true);
    await waitFor(() => {
      expect(screen.queryByRole('link', { name: 'Vazgeç' })).not.toBeInTheDocument();
    });
  });

  it('zorunlu olmadığında Vazgeç gösterilir', async () => {
    renderPage(false);
    expect(await screen.findByRole('link', { name: 'Vazgeç' })).toBeInTheDocument();
  });
});
