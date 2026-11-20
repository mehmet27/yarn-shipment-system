import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { http, HttpResponse } from 'msw';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

const EMPTY_PAGE = { items: [], page: { number: 0, size: 20, totalElements: 0, totalPages: 0 } };

describe('ShipmentListPage dar ekran', () => {
  beforeEach(() => {
    server.use(...createHandlers('MANAGER'));
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('360 px genişlikte tablo yerine dokunulabilir kartlar gösterir', async () => {
    const narrow = (query: string) => ({
      matches: !query.includes('min-width'),
      media: query,
      addEventListener: () => undefined,
      removeEventListener: () => undefined,
      addListener: () => undefined,
      removeListener: () => undefined,
    });
    vi.stubGlobal('matchMedia', narrow);
    renderApp('/panel/sevkiyat');
    expect(
      await screen.findByRole('link', { name: 'SVK-2026-0001' }, { timeout: 10_000 }),
    ).toHaveAttribute('href', '/panel/sevkiyat/shp-0001');
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });
});

describe('ShipmentListPage', () => {
  beforeEach(() => {
    server.use(...createHandlers('MANAGER'));
  });

  it('ilk sayfada en yeni 20 sevkiyatı listeler', async () => {
    renderApp('/panel/sevkiyat');
    const table = await screen.findByRole('table');
    expect(within(table).getAllByRole('row')).toHaveLength(21);
    expect(within(table).getByRole('link', { name: 'SVK-2026-0001' })).toHaveAttribute(
      'href',
      '/panel/sevkiyat/shp-0001',
    );
  });

  it('durum filtresini adrese yazar ve listeyi süzer', async () => {
    const { router } = renderApp('/panel/sevkiyat?sayfa=2');
    await userEvent.click(await screen.findByPlaceholderText('Tüm durumlar'));
    await userEvent.click(await screen.findByRole('option', { name: 'Tamamlandı' }));
    expect(router.state.location.search).toBe('?durum=COMPLETED');
    const table = await screen.findByRole('table');
    expect(await within(table).findAllByText('Tamamlandı')).toHaveLength(11);
  });

  it('sonraki sayfaya geçince sayfa numarasını adrese yazar', async () => {
    const { router } = renderApp('/panel/sevkiyat');
    await userEvent.click(await screen.findByRole('button', { name: '2' }));
    expect(router.state.location.search).toBe('?sayfa=2');
    expect(await screen.findByRole('link', { name: 'SVK-2026-0021' })).toBeInTheDocument();
  });

  it('hiç sevkiyat yoksa müdüre "Yeni sevkiyat" düğmesiyle söyler', async () => {
    server.use(http.get('/api/v1/shipments', () => HttpResponse.json(EMPTY_PAGE)));
    renderApp('/panel/sevkiyat');
    expect(await screen.findByText('Henüz sevkiyat yok')).toBeInTheDocument();
    expect(screen.getAllByRole('link', { name: 'Yeni sevkiyat' })).toHaveLength(2);
  });

  it('süzgece uyan sevkiyat yoksa "Tüm durumları göster" süzgeci kaldırır', async () => {
    server.use(
      http.get('/api/v1/shipments', ({ request }) =>
        new URL(request.url).searchParams.has('status') ? HttpResponse.json(EMPTY_PAGE) : undefined,
      ),
    );
    renderApp('/panel/sevkiyat?durum=CANCELLED');
    expect(await screen.findByText('Bu durumda sevkiyat yok.')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Tüm durumları göster' }));
    expect(await screen.findByRole('table')).toBeInTheDocument();
  });
});
