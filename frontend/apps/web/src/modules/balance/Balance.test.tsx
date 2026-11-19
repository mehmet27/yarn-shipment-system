import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { BALANCES_PATH } from '@iplik/api-client/settlements';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { addMockReceipt, markMockInvoiced } from '@/mocks/settlements';
import { renderApp } from '@/test/renderApp';

describe('Fasoncu bakiyesi', () => {
  it('kalite matrisi ve hareketleri görür', async () => {
    server.use(...createHandlers('QUALITY'));
    renderApp('/panel/bakiye');
    const table = await screen.findByRole('table', { name: 'Fasoncu bakiyesi' });
    expect(within(table).getByText('30/1 Penye')).toBeInTheDocument();
    expect(within(table).getByText('Aydın Örme')).toBeInTheDocument();
    expect(await screen.findByText('Hareket yok.')).toBeInTheDocument();
  });

  it('hareketi giren kişinin adı yazılır; ad çözülemezse "Bir çalışan"', async () => {
    const move = {
      id: 'm1',
      subcontractorId: 'sub-0',
      yarnType: '30/1 Penye',
      kind: 'DISPATCH',
      netKg: '100',
      occurredAt: '2026-11-17T08:00:00Z',
    };
    server.use(
      http.get(`${BALANCES_PATH}/movements`, () =>
        HttpResponse.json({
          items: [
            { ...move, createdBy: 'u-1' },
            { ...move, id: 'm2', createdBy: 'u-2' },
          ],
          page: { number: 0, size: 20, totalElements: 2, totalPages: 1 },
          actorNames: { 'u-1': 'Veli Müdür' },
        }),
      ),
      ...createHandlers('QUALITY'),
    );
    renderApp('/panel/bakiye');
    expect(await screen.findByText('Veli Müdür')).toBeInTheDocument();
    expect(screen.getByText('Bir çalışan')).toBeInTheDocument();
  });

  it('menüde yalnızca izni olana görünür', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat');
    await screen.findByRole('link', { name: 'Sevkiyatlar' });
    expect(screen.queryByRole('link', { name: 'Fasoncu bakiyesi' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Faturalama' })).not.toBeInTheDocument();
  });

  it('izinsiz rol bakiye sayfasında yönlendirilir', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/bakiye');
    expect(await screen.findByText(/yetkiniz yok|Erişim/i)).toBeInTheDocument();
  });
});

describe('Faturalama: Faturalanacaklar sekmesi', () => {
  it('müdür hatalı kilosu olan dosyayı listeler ve dosyaya gider', async () => {
    addMockReceipt(
      'shp-0003',
      { fabricType: 'Süprem', netKg: '1900', waybillNo: 'K-1' },
      'user-quality',
    );
    server.use(...createHandlers('MANAGER'));
    const { router } = renderApp('/panel/sevkiyat/shp-0003');
    await userEvent.click(await screen.findByRole('link', { name: 'Faturalama' }));
    const card = await screen.findByRole('article', { name: 'SVK-2026-0003' });
    expect(within(card).getByText(/Faturalanacak kilo 3,2 kg/)).toBeInTheDocument();
    await userEvent.click(within(card).getByRole('link', { name: 'SVK-2026-0003' }));
    expect(router.state.location.pathname).toBe('/panel/sevkiyat/shp-0003');
  });

  it('müdür kalandan az kilo için fatura girer: dosya listede kalır, kalan kilo azalır', async () => {
    addMockReceipt(
      'shp-0003',
      { fabricType: 'Süprem', netKg: '1900', waybillNo: 'K-1' },
      'user-quality',
    );
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/faturalama');
    const card = await screen.findByRole('article', { name: 'SVK-2026-0003' });
    await userEvent.click(within(card).getByRole('button', { name: 'Fatura bilgisini gir' }));
    const dialog = await screen.findByRole('dialog', { name: /Fatura bilgisi/ });
    await userEvent.type(await within(dialog).findByLabelText(/Fatura no/), 'F-1');
    const kg = within(dialog).getByLabelText(/\(kg\)/);
    await userEvent.clear(kg);
    await userEvent.type(kg, '1');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Fatura edildi' }));
    await userEvent.click(within(dialog).getByRole('button', { name: 'Evet, fatura edildi' }));
    expect(await screen.findByText(/Faturalanacak kilo 2,2 kg/)).toBeInTheDocument();
    expect(screen.getByText(/Faturası kesilen: 1 kg/)).toBeInTheDocument();
  });

  it('kalandan fazla kilo yazılırsa istek gitmez ve uyarı çıkar', async () => {
    addMockReceipt(
      'shp-0003',
      { fabricType: 'Süprem', netKg: '1900', waybillNo: 'K-1' },
      'user-quality',
    );
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/faturalama');
    const card = await screen.findByRole('article', { name: 'SVK-2026-0003' });
    await userEvent.click(within(card).getByRole('button', { name: 'Fatura bilgisini gir' }));
    const dialog = await screen.findByRole('dialog', { name: /Fatura bilgisi/ });
    await userEvent.type(await within(dialog).findByLabelText(/Fatura no/), 'F-1');
    const kg = within(dialog).getByLabelText(/\(kg\)/);
    await userEvent.clear(kg);
    await userEvent.type(kg, '9');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Fatura edildi' }));
    expect(
      await within(dialog).findByText(/faturası kesilmemiş kilodan fazla/),
    ).toBeInTheDocument();
  });

  it('müdür satırdan fatura bilgisini girer; dosya listeden düşer', async () => {
    addMockReceipt(
      'shp-0003',
      { fabricType: 'Süprem', netKg: '1900', waybillNo: 'K-1' },
      'user-quality',
    );
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/faturalama');
    const card = await screen.findByRole('article', { name: 'SVK-2026-0003' });
    await userEvent.click(within(card).getByRole('button', { name: 'Fatura bilgisini gir' }));
    const dialog = await screen.findByRole('dialog', { name: /Fatura bilgisi/ });
    await userEvent.type(within(dialog).getByLabelText(/Fatura no/), 'F-1');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Fatura edildi' }));
    await userEvent.click(within(dialog).getByRole('button', { name: 'Evet, fatura edildi' }));
    await waitFor(() => {
      expect(screen.queryByRole('article', { name: 'SVK-2026-0003' })).not.toBeInTheDocument();
    });
  });

  it('pencerede aynı sevkiyata daha önce girilen faturalar görünür', async () => {
    addMockReceipt(
      'shp-0003',
      { fabricType: 'Süprem', netKg: '1900', waybillNo: 'K-1' },
      'user-quality',
    );
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/faturalama');
    const card = await screen.findByRole('article', { name: 'SVK-2026-0003' });
    await userEvent.click(within(card).getByRole('button', { name: 'Fatura bilgisini gir' }));
    let dialog = await screen.findByRole('dialog', { name: /Fatura bilgisi/ });
    expect(await within(dialog).findByText(/henüz fatura girilmedi/)).toBeInTheDocument();
    await userEvent.type(within(dialog).getByLabelText(/Fatura no/), 'F-7');
    const kg = within(dialog).getByLabelText(/\(kg\)/);
    await userEvent.clear(kg);
    await userEvent.type(kg, '1');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Fatura edildi' }));
    await userEvent.click(within(dialog).getByRole('button', { name: 'Evet, fatura edildi' }));
    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
    await userEvent.click(
      within(await screen.findByRole('article', { name: 'SVK-2026-0003' })).getByRole('button', {
        name: 'Fatura bilgisini gir',
      }),
    );
    dialog = await screen.findByRole('dialog', { name: /Fatura bilgisi/ });
    expect(await within(dialog).findByText(/F-7 · .* · 1 kg/)).toBeInTheDocument();
  });
});

function seedInvoices(): void {
  addMockReceipt(
    'shp-0003',
    { fabricType: 'Süprem', netKg: '1900', waybillNo: 'K-1' },
    'user-quality',
  );
  const base = { lines: [], userId: 'user-manager' } as const;
  markMockInvoiced('shp-0003', {
    ...base,
    invoiceNo: 'F-10',
    invoicedAt: '2026-11-05',
    lines: [{ yarnType: '30/1 Penye', netKg: '1' }],
  });
  markMockInvoiced('shp-0003', {
    ...base,
    invoiceNo: 'F-11',
    invoicedAt: '2026-11-12',
    lines: [{ yarnType: '30/1 Penye', netKg: '1.5' }],
  });
}

describe('Faturalama: Faturalananlar sekmesi', () => {
  it('girilmiş faturaları yeniden eskiye listeler; toplam kilo ve sevkiyat bağlantısı görünür', async () => {
    seedInvoices();
    server.use(...createHandlers('MANAGER'));
    const { router } = renderApp('/panel/faturalama');
    await userEvent.click(await screen.findByRole('tab', { name: 'Faturalananlar' }));
    const table = await screen.findByRole('table', { name: 'Faturalananlar' });
    const rows = within(table).getAllByRole('row');
    expect(rows.map((row) => row.textContent)).toEqual([
      expect.stringContaining('Fatura no'),
      expect.stringMatching(/F-11.*12\.11\.2026.*1,5 kg/),
      expect.stringMatching(/F-10.*05\.11\.2026.*1 kg/),
    ]);
    expect(screen.getByText('2 fatura · toplam 2,5 kg')).toBeInTheDocument();
    expect(router.state.location.search).toBe('?sekme=faturalananlar');
    await userEvent.click(
      within(table).getAllByRole('link', { name: 'SVK-2026-0003' })[0] ?? table,
    );
    expect(router.state.location.pathname).toBe('/panel/sevkiyat/shp-0003');
  });

  it('tarih aralığına göre süzülür; toplam süzgece uyanlardan hesaplanır', async () => {
    seedInvoices();
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/faturalama?sekme=faturalananlar');
    await screen.findByText('2 fatura · toplam 2,5 kg');
    await userEvent.type(screen.getByLabelText('Başlangıç tarihi'), '2026-11-10');
    expect(await screen.findByText('1 fatura · toplam 1,5 kg')).toBeInTheDocument();
    expect(screen.queryByText('F-10')).not.toBeInTheDocument();
    await userEvent.type(screen.getByLabelText('Bitiş tarihi'), '2026-11-11');
    expect(await screen.findByText('Bu süzgece uyan fatura yok.')).toBeInTheDocument();
  });

  it('başlangıç tarihi bitişten sonraysa uyarı çıkar ve Excel kapanır', async () => {
    seedInvoices();
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/faturalama?sekme=faturalananlar');
    await screen.findByText('2 fatura · toplam 2,5 kg');
    await userEvent.type(screen.getByLabelText('Bitiş tarihi'), '2026-11-01');
    await userEvent.type(screen.getByLabelText('Başlangıç tarihi'), '2026-11-20');
    expect(
      await screen.findByText(/Başlangıç tarihi bitiş tarihinden sonra olamaz/),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: "Excel'e al" })).toBeDisabled();
  });

  it('henüz fatura yoksa boş durum yazar', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/faturalama?sekme=faturalananlar');
    expect(await screen.findByText('Bu süzgece uyan fatura yok.')).toBeInTheDocument();
  });

  it('eski adres yeni ekrana yönlenir', async () => {
    server.use(...createHandlers('MANAGER'));
    const { router } = renderApp('/panel/faturalanacaklar');
    expect(await screen.findByRole('tab', { name: 'Faturalanacaklar' })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/panel/faturalama');
  });
});
