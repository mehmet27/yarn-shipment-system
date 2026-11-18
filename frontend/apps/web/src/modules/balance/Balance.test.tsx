import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { BALANCES_PATH } from '@iplik/api-client/settlements';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { addMockReceipt } from '@/mocks/settlements';
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
    expect(screen.queryByRole('link', { name: 'Faturalanacaklar' })).not.toBeInTheDocument();
  });

  it('izinsiz rol bakiye sayfasında yönlendirilir', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/bakiye');
    expect(await screen.findByText(/yetkiniz yok|Erişim/i)).toBeInTheDocument();
  });
});

describe('Faturalanacaklar', () => {
  it('müdür hatalı kilosu olan dosyayı listeler ve dosyaya gider', async () => {
    addMockReceipt(
      'shp-0003',
      { fabricType: 'Süprem', netKg: '1900', waybillNo: 'K-1' },
      'user-quality',
    );
    server.use(...createHandlers('MANAGER'));
    const { router } = renderApp('/panel/sevkiyat/shp-0003');
    await userEvent.click(await screen.findByRole('link', { name: 'Faturalanacaklar' }));
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
    renderApp('/panel/faturalanacaklar');
    const card = await screen.findByRole('article', { name: 'SVK-2026-0003' });
    await userEvent.click(within(card).getByRole('button', { name: 'Fatura bilgisini gir' }));
    await userEvent.type(await within(card).findByLabelText(/Fatura no/), 'F-1');
    const kg = within(card).getByLabelText(/\(kg\)/);
    await userEvent.clear(kg);
    await userEvent.type(kg, '1');
    await userEvent.click(within(card).getByRole('button', { name: 'Fatura edildi' }));
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
    renderApp('/panel/faturalanacaklar');
    const card = await screen.findByRole('article', { name: 'SVK-2026-0003' });
    await userEvent.click(within(card).getByRole('button', { name: 'Fatura bilgisini gir' }));
    await userEvent.type(await within(card).findByLabelText(/Fatura no/), 'F-1');
    const kg = within(card).getByLabelText(/\(kg\)/);
    await userEvent.clear(kg);
    await userEvent.type(kg, '9');
    await userEvent.click(within(card).getByRole('button', { name: 'Fatura edildi' }));
    expect(await within(card).findByText(/faturası kesilmemiş kilodan fazla/)).toBeInTheDocument();
  });

  it('müdür satırdan fatura bilgisini girer; dosya listeden düşer', async () => {
    addMockReceipt(
      'shp-0003',
      { fabricType: 'Süprem', netKg: '1900', waybillNo: 'K-1' },
      'user-quality',
    );
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/faturalanacaklar');
    const card = await screen.findByRole('article', { name: 'SVK-2026-0003' });
    await userEvent.click(within(card).getByRole('button', { name: 'Fatura bilgisini gir' }));
    await userEvent.type(within(card).getByLabelText(/Fatura no/), 'F-1');
    await userEvent.click(within(card).getByRole('button', { name: 'Fatura edildi' }));
    await waitFor(() => {
      expect(screen.queryByRole('article', { name: 'SVK-2026-0003' })).not.toBeInTheDocument();
    });
  });
});
