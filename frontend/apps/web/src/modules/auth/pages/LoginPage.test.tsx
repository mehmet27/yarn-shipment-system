import { UiProvider } from '@iplik/ui';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, Route, Routes } from 'react-router';
import { describe, expect, it } from 'vitest';
import { server } from '@/mocks/server';
import { LoginPage } from './LoginPage';

function renderPage(url = '/giris') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <UiProvider env="test">
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={[url]}>
          <Routes>
            <Route path="/giris" element={<LoginPage />} />
            <Route path="/sifre-degistir" element={<p>şifre sayfası</p>} />
            <Route path="/panel" element={<p>panel</p>} />
            <Route path="/" element={<p>ana sayfa</p>} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    </UiProvider>,
  );
}

async function fillAndSubmit() {
  await userEvent.type(screen.getByLabelText(/Kullanıcı adı/), 'depo');
  await userEvent.type(screen.getByLabelText(/Şifre/), 'gizli-sifre');
  await userEvent.click(screen.getByRole('button', { name: 'Giriş yap' }));
}

describe('LoginPage', () => {
  it('girişten sonra geldiği sayfaya döner', async () => {
    server.use(
      http.post('/api/v1/auth/login', () => HttpResponse.json({ passwordChangeRequired: false })),
    );
    renderPage('/giris?returnTo=%2Fpanel');
    await fillAndSubmit();
    expect(await screen.findByText('panel')).toBeInTheDocument();
  });

  it('geçici şifreyle girenleri şifre değiştirme sayfasına götürür', async () => {
    server.use(
      http.post('/api/v1/auth/login', () => HttpResponse.json({ passwordChangeRequired: true })),
    );
    renderPage();
    await fillAndSubmit();
    expect(await screen.findByText('şifre sayfası')).toBeInTheDocument();
  });

  it('hatalı girişte hangisinin yanlış olduğunu söylemeden uyarır', async () => {
    server.use(
      http.post('/api/v1/auth/login', () =>
        HttpResponse.json({ code: 'AUTHENTICATION_FAILED', status: 401 }, { status: 401 }),
      ),
    );
    renderPage();
    await fillAndSubmit();
    expect(await screen.findByText('Kullanıcı adı ya da şifre hatalı.')).toBeInTheDocument();
  });

  it('kilitli hesabı bildirir', async () => {
    server.use(
      http.post('/api/v1/auth/login', () =>
        HttpResponse.json({ code: 'ACCOUNT_LOCKED', status: 423 }, { status: 423 }),
      ),
    );
    renderPage();
    await fillAndSubmit();
    expect(await screen.findByText(/hesap kısa süre kilitlendi/)).toBeInTheDocument();
  });
});
