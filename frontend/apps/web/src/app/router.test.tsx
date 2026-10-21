import type { Role } from '@iplik/api-client';
import { screen, waitFor } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { createMeHandler } from '../mocks/handlers';
import { server } from '../mocks/server';
import { renderApp } from '../test/renderApp';

describe('appRoutes', () => {
  it.each<[Role, string]>([
    ['ADMIN', '/panel'],
    ['MANAGER', '/panel'],
    ['DRIVER', '/saha'],
    ['SUBCONTRACTOR', '/saha'],
  ])('%s rolü ana sayfadan %s kabuğuna yönlenir', async (role, expectedPath) => {
    server.use(createMeHandler(role));
    const { router } = renderApp('/');
    await waitFor(() => {
      expect(router.state.location.pathname).toBe(expectedPath);
    });
  });

  it('panelde kullanıcıyı adıyla karşılar', async () => {
    renderApp('/panel');
    expect(await screen.findByRole('heading', { name: /Ayşe Yönetici/ })).toBeInTheDocument();
  });

  it('bilinmeyen adreste bulunamadı sayfası gösterir', () => {
    renderApp('/olmayan-sayfa');
    expect(screen.getByText('Aradığınız sayfa bulunamadı.')).toBeInTheDocument();
  });
});
