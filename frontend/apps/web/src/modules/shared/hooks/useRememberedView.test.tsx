import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

const REPORTS_KEY = 'iplik.view.user-manager.raporlar';
const SHIPMENTS_KEY = 'iplik.view.user-manager.sevkiyatlar';
/** Raporlar ekranı ağırdır; yavaş CI koşucusunda ilk çizim varsayılan 1 saniyeyi aşabilir. */
const SLOW = { timeout: 10_000 };

describe('useRememberedView', () => {
  it('raporlarda seçilen sekme ve dönem adrese yazılır ve saklanır', async () => {
    server.use(...createHandlers('MANAGER'));
    const { router } = renderApp('/panel/raporlar');
    await userEvent.click(await screen.findByRole('tab', { name: 'Fire raporu' }, SLOW));
    await userEvent.click(screen.getByText('90 gün'));
    await waitFor(() => {
      expect(router.state.location.search).toBe('?sekme=fire&gun=90');
    });
    await waitFor(() => {
      expect(window.localStorage.getItem(REPORTS_KEY)).toBe('sekme=fire&gun=90');
    });
  });

  it('raporlar süzgeçsiz açılınca kişinin son görünümü geri gelir', async () => {
    window.localStorage.setItem(REPORTS_KEY, 'sekme=fire&gun=90');
    server.use(...createHandlers('MANAGER'));
    const { router } = renderApp('/panel/raporlar');
    expect(
      await screen.findByRole('tab', { name: 'Fire raporu', selected: true }, SLOW),
    ).toBeVisible();
    expect(router.state.location.search).toBe('?sekme=fire&gun=90');
  });

  it('başka kullanıcının saklanan görünümü yüklenmez', async () => {
    window.localStorage.setItem('iplik.view.user-quality.raporlar', 'sekme=fire');
    server.use(...createHandlers('MANAGER'));
    const { router } = renderApp('/panel/raporlar');
    expect(await screen.findByRole('tab', { name: 'Özet', selected: true }, SLOW)).toBeVisible();
    expect(router.state.location.search).toBe('');
  });

  it('adres bir süzgeç taşıyorsa son görünüm onun yerine geçmez, yenisi saklanır', async () => {
    window.localStorage.setItem(SHIPMENTS_KEY, 'durum=DRAFT');
    server.use(...createHandlers('MANAGER'));
    const { router } = renderApp('/panel/sevkiyat?durum=COMPLETED');
    await screen.findByRole('heading', { name: 'Sevkiyatlar' }, SLOW);
    await waitFor(() => {
      expect(window.localStorage.getItem(SHIPMENTS_KEY)).toBe('durum=COMPLETED');
    });
    expect(router.state.location.search).toBe('?durum=COMPLETED');
  });

  it('sayfa taşıyan bağlantı (ana sayfadaki "Tümünü gör") son süzgeci geri yüklemez', async () => {
    window.localStorage.setItem(SHIPMENTS_KEY, 'durum=DRAFT');
    server.use(...createHandlers('MANAGER'));
    const { router } = renderApp('/panel/sevkiyat?sayfa=1');
    await screen.findByRole('heading', { name: 'Sevkiyatlar' }, SLOW);
    expect(router.state.location.search).toBe('?sayfa=1');
    expect(window.localStorage.getItem(SHIPMENTS_KEY)).toBe('durum=DRAFT');
  });

  it('sevkiyat listesi süzgeçsiz açılınca son süzgeç geri yüklenir', async () => {
    window.localStorage.setItem(SHIPMENTS_KEY, 'durum=DRAFT');
    server.use(...createHandlers('MANAGER'));
    const { router } = renderApp('/panel/sevkiyat');
    await waitFor(() => {
      expect(router.state.location.search).toBe('?durum=DRAFT');
    }, SLOW);
  });
});
