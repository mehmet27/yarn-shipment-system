import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
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
    await userEvent.click(await screen.findByRole('tab', { name: 'Şoförler' }));
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

  it('raporlar ekranı Özet sekmesiyle açılır ve toplam satırı ağırlıklı oranı gösterir', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/raporlar');
    expect(await screen.findByTestId('stat-total')).toHaveTextContent('12');
    await userEvent.click(screen.getByRole('tab', { name: 'Şoförler' }));
    const total = await screen.findByTestId('party-total');
    expect(total).toHaveTextContent('7');
    expect(total).toHaveTextContent('%28,57');
  });

  it('"yalnızca uyuşmazlıklılar" anahtarı uyuşmazlığı olmayan satırı gizler', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/raporlar');
    await userEvent.click(await screen.findByRole('tab', { name: 'Şoförler' }));
    await screen.findByText('b2c3d4e5');
    await userEvent.click(screen.getByLabelText('Yalnızca uyuşmazlıklılar'));
    expect(screen.queryByText('b2c3d4e5')).not.toBeInTheDocument();
    expect(screen.getByText('a1b2c3d4')).toBeInTheDocument();
  });

  it('başlangıç bitişten sonraysa uyarır ve dışa aktarma düğmelerini kapatır', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/raporlar');
    await userEvent.type(await screen.findByLabelText('Başlangıç tarihi'), '2026-11-10');
    await userEvent.type(screen.getByLabelText('Bitiş tarihi'), '2026-11-01');
    expect(await screen.findByText('Başlangıç, bitişten sonra olamaz.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Excel indir (.xlsx)' })).toBeDisabled();
  });

  it('PDF düğmesi yazdırılabilir raporu imza alanlarıyla hazırlar ve yazdırmayı açar', async () => {
    server.use(...createHandlers('MANAGER'));
    const print = vi.spyOn(window, 'print').mockImplementation(() => undefined);
    renderApp('/panel/raporlar');
    await userEvent.click(await screen.findByRole('button', { name: 'PDF / Yazdır' }));
    await waitFor(() => {
      expect(print).toHaveBeenCalledTimes(1);
    });
    print.mockRestore();
  });

  it('Excel düğmesi dosyayı indirir', async () => {
    server.use(...createHandlers('MANAGER'));
    const create = vi.fn(() => 'blob:rapor');
    vi.stubGlobal('URL', Object.assign(URL, { createObjectURL: create, revokeObjectURL: vi.fn() }));
    const click = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(() => undefined);
    renderApp('/panel/raporlar');
    await userEvent.click(await screen.findByRole('button', { name: 'Excel indir (.xlsx)' }));
    await waitFor(() => {
      expect(click).toHaveBeenCalledTimes(1);
    });
    expect(create).toHaveBeenCalledTimes(1);
    click.mockRestore();
    vi.unstubAllGlobals();
  });

  it('şoför ve fasoncu adlarını tanım listesinden çözer', async () => {
    server.use(
      http.get('/api/v1/master-data/drivers', () =>
        HttpResponse.json({
          items: [{ id: 'a1b2c3d4-0000-7000-8000-000000000001', fullName: 'Ali Veli' }],
          page: { number: 0, size: 100, totalElements: 1, totalPages: 1 },
        }),
      ),
      ...createHandlers('MANAGER'),
    );
    renderApp('/panel/raporlar');
    await userEvent.click(await screen.findByRole('tab', { name: 'Şoförler' }));
    expect(await screen.findByText('Ali Veli')).toBeInTheDocument();
    expect(screen.getByText('b2c3d4e5')).toBeInTheDocument();
  });

  it('logo eklenir, geçersiz dosya reddedilir ve yazdırma görünümüne girer', async () => {
    server.use(...createHandlers('MANAGER'));
    const print = vi.spyOn(window, 'print').mockImplementation(() => undefined);
    renderApp('/panel/raporlar');
    await screen.findByRole('button', { name: 'Rapor logosu ekle' });
    const input = screen.getByLabelText('Logo dosyası');
    await userEvent.upload(input, new File(['x'], 'a.gif', { type: 'image/gif' }), {
      applyAccept: false,
    });
    expect(await screen.findByRole('alert')).toHaveTextContent('PNG veya JPEG');
    await userEvent.upload(input, new File(['x'], 'logo.png', { type: 'image/png' }));
    expect(await screen.findByRole('button', { name: 'Logoyu kaldır' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'PDF / Yazdır' }));
    await waitFor(() => {
      expect(print).toHaveBeenCalled();
    });
    await userEvent.click(screen.getByRole('button', { name: 'Logoyu kaldır' }));
    expect(screen.getByRole('button', { name: 'Rapor logosu ekle' })).toBeInTheDocument();
    window.localStorage.clear();
  });
});
