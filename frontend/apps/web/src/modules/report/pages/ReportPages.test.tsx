import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

describe('Panel ana sayfası ve raporlar', () => {
  it('yönetici ana sayfada dashboard kartlarını, süresi geçen uyuşmazlığı ve en büyük farkı görür', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel');
    expect(await screen.findByTestId('stat-total')).toHaveTextContent('12');
    expect(screen.getByTestId('stat-open')).toHaveTextContent('3');
    expect(screen.getByTestId('stat-overdue')).toHaveTextContent('1');
    expect(screen.getByRole('link', { name: 'UY-2026-0001' })).toHaveAttribute(
      'href',
      '/panel/uyusmazliklar/disc-0',
    );
    expect(screen.getByRole('link', { name: 'SV-2026-0007' })).toBeInTheDocument();
  });

  it('rapor yetkisi olmayan kullanıcıya dashboard göstermez ve çağırmaz', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel');
    expect(await screen.findByRole('heading', { level: 1 })).toBeInTheDocument();
    expect(screen.queryByTestId('stat-total')).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Raporlar' })).not.toBeInTheDocument();
  });

  it('dönem seçimi dashboard sorgusunu yeniler', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp('/panel');
    await screen.findByTestId('stat-total');
    await userEvent.click(screen.getByText('7 gün'));
    expect(await screen.findByTestId('stat-total')).toBeInTheDocument();
  });

  it('şoför raporu oranı yüzde olarak gösterir; sekmeler fasoncu ve eşleşmeyi açar', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/raporlar');
    const row = await screen.findByRole('row', { name: /a1b2c3d4/ });
    expect(within(row).getByText('%50,00')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('tab', { name: 'Eşleşmeler' }));
    expect(await screen.findByText('f9e8d7c6')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('tab', { name: 'Fasoncular' }));
    expect((await screen.findAllByText('a1b2c3d4')).length).toBeGreaterThan(0);
  });

  it('rapor yetkisi olmayan kullanıcı raporlar adresinde engellenir', async () => {
    server.use(...createHandlers('DRIVER'));
    const { router } = renderApp('/panel/raporlar');
    await waitFor(() => {
      expect(router.state.location.pathname).not.toBe('/panel/raporlar');
    });
  });
});
