import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
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
    expect(within(card).getByText(/Hatalı kilo 3,2 kg/)).toBeInTheDocument();
    await userEvent.click(within(card).getByRole('link', { name: 'SVK-2026-0003' }));
    expect(router.state.location.pathname).toBe('/panel/sevkiyat/shp-0003');
  });
});
