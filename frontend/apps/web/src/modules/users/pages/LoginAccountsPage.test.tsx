import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

const PATH = '/api/v1/master-data/login-accounts';

describe('Kullanıcılar ekranı', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('yönetici giriş hesaplarını kullanıcı adı, rol ve durumla görür; şifre gösterilmez', async () => {
    renderApp('/panel/kullanicilar');
    const table = await screen.findByRole('table');
    const rows = within(table).getAllByRole('row');
    expect(rows).toHaveLength(7);
    const partner = rows.find((row) => within(row).queryByText('kalite') !== null);
    expect(partner).toBeDefined();
    if (partner === undefined) return;
    expect(within(partner).getByText('Kalite Uzmanı')).toBeInTheDocument();
    expect(within(partner).getByText('Kalite')).toBeInTheDocument();
    expect(within(partner).getByText('Etkin')).toBeInTheDocument();
    expect(within(partner).getByText('İlk giriş bekleniyor')).toBeInTheDocument();
    expect(within(table).getByText('Devre dışı')).toBeInTheDocument();
    expect(screen.getByText('6 kullanıcı')).toBeInTheDocument();
    expect(screen.queryByText(/şifre:/i)).not.toBeInTheDocument();
  });

  it('menüde Kullanıcılar bağlantısı yöneticide görünür', async () => {
    renderApp('/panel');
    expect(await screen.findByRole('link', { name: 'Kullanıcılar' })).toHaveAttribute(
      'href',
      '/panel/kullanicilar',
    );
  });

  it('arama kullanıcı adı ve adda yapılır; sonuç yoksa bilgi verir', async () => {
    renderApp('/panel/kullanicilar');
    const search = await screen.findByRole('searchbox', { name: /Giriş adı ya da ad/ });
    await userEvent.type(search, 'kalite uzm');
    expect(await screen.findByText('1 kullanıcı')).toBeInTheDocument();
    expect(screen.getByText('kalite')).toBeInTheDocument();

    await userEvent.clear(search);
    await userEvent.type(search, 'bulunmayan');
    expect(await screen.findByText('Aramaya uyan kullanıcı bulunamadı.')).toBeInTheDocument();
  });

  it('hiç hesap yoksa boş liste bilgisini gösterir', async () => {
    server.use(
      http.get(PATH, () =>
        HttpResponse.json({
          items: [],
          page: { number: 0, size: 20, totalElements: 0, totalPages: 0 },
        }),
      ),
    );
    renderApp('/panel/kullanicilar');
    expect(await screen.findByText('Giriş hesabı olan kullanıcı yok.')).toBeInTheDocument();
  });

  it('giriş sistemine ulaşılamazsa hata ve yeniden dene gösterir', async () => {
    server.use(
      http.get(
        PATH,
        () =>
          HttpResponse.json(
            { status: 502, code: 'LOGIN_ACCOUNT_UNAVAILABLE', title: 'Giriş sistemi' },
            { status: 502 },
          ),
        { once: true },
      ),
    );
    renderApp('/panel/kullanicilar');
    expect(
      await screen.findByText('Giriş sistemine şu an ulaşılamıyor; kullanıcı listesi alınamadı.'),
    ).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Yeniden dene' }));
    expect(await screen.findByRole('table')).toBeInTheDocument();
  });

  it('dar ekranda tablo yerine kullanıcı kartları gösterir', async () => {
    const narrow = (query: string) => ({
      matches: !query.includes('min-width'),
      media: query,
      addEventListener: () => undefined,
      removeEventListener: () => undefined,
      addListener: () => undefined,
      removeListener: () => undefined,
    });
    vi.stubGlobal('matchMedia', narrow);
    renderApp('/panel/kullanicilar');
    const card = await screen.findByRole('group', { name: 'admin' });
    expect(within(card).getByText('Yönetici')).toBeInTheDocument();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  it('yönetici olmayan rol menüde ekranı görmez ve adrese gidince engellenir', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    const { router } = renderApp('/panel/kullanicilar');
    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/403');
    });
    expect(screen.queryByRole('link', { name: 'Kullanıcılar' })).not.toBeInTheDocument();
  });

  it('durum süzgeci yalnız devre dışı hesapları gösterir; Açılış sütunu yoktur', async () => {
    renderApp('/panel/kullanicilar');
    const table = await screen.findByRole('table');
    expect(within(table).queryByText('Açılış')).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('radio', { name: 'Devre dışı' }));
    expect(await screen.findByText('Bu sayfada 1 kullanıcı')).toBeInTheDocument();
    expect(within(screen.getByRole('table')).getByText('eski.depo')).toBeInTheDocument();
  });
});
