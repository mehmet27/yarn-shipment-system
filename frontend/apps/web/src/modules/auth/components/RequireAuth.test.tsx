import { ME_PATH } from '@iplik/api-client';
import { UiProvider } from '@iplik/ui';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, Route, Routes } from 'react-router';
import { describe, expect, it } from 'vitest';
import { createMockUser } from '@/mocks/users';
import { server } from '@/mocks/server';
import { RequireAuth } from './RequireAuth';

function renderGuard(allowPasswordChangeRequired = false) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <UiProvider env="test">
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/panel?x=1']}>
          <Routes>
            <Route
              path="/panel"
              element={
                <RequireAuth
                  errorFallback={<p>hata</p>}
                  allowPasswordChangeRequired={allowPasswordChangeRequired}
                >
                  <p>içerik</p>
                </RequireAuth>
              }
            />
            <Route path="/giris" element={<p>giriş sayfası</p>} />
            <Route path="/sifre-degistir" element={<p>şifre sayfası</p>} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    </UiProvider>,
  );
}

describe('RequireAuth', () => {
  it('oturum yoksa giriş sayfasına yönlendirir', async () => {
    server.use(http.get(ME_PATH, () => HttpResponse.json({ status: 401 }, { status: 401 })));
    renderGuard();
    expect(await screen.findByText('giriş sayfası')).toBeInTheDocument();
  });

  it('oturum varsa içeriği gösterir', async () => {
    server.use(http.get(ME_PATH, () => HttpResponse.json(createMockUser('ADMIN'))));
    renderGuard();
    expect(await screen.findByText('içerik')).toBeInTheDocument();
  });

  it('geçici şifreyle girenleri şifre değiştirmeye yönlendirir', async () => {
    server.use(
      http.get(ME_PATH, () =>
        HttpResponse.json({ ...createMockUser('ADMIN'), passwordChangeRequired: true }),
      ),
    );
    renderGuard();
    expect(await screen.findByText('şifre sayfası')).toBeInTheDocument();
  });

  it('şifre değiştirme sayfasında geçici şifreli kişiye içeriği gösterir', async () => {
    server.use(
      http.get(ME_PATH, () =>
        HttpResponse.json({ ...createMockUser('ADMIN'), passwordChangeRequired: true }),
      ),
    );
    renderGuard(true);
    expect(await screen.findByText('içerik')).toBeInTheDocument();
  });

  it('beklenmeyen hatada hata ekranını gösterir', async () => {
    server.use(http.get(ME_PATH, () => HttpResponse.json({ status: 500 }, { status: 500 })));
    renderGuard();
    expect(await screen.findByText('hata')).toBeInTheDocument();
  });
});
