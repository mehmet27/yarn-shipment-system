import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
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
    server.use(...createHandlers('MANAGER'));
    const { router } = renderApp('/panel/sevkiyat/shp-0003');
    await userEvent.click(await screen.findByRole('tab', { name: 'Kumaş girişi' }));
    const form = await screen.findByRole('region', { name: 'Kumaş girişi' });
    await userEvent.type(within(form).getByLabelText(/Kumaş cinsi/), 'Süprem');
    await userEvent.type(within(form).getByLabelText(/Kumaş net kilosu/), '1.900');
    await userEvent.type(within(form).getByLabelText(/İrsaliye no/), 'K-1');
    await userEvent.click(within(form).getByRole('button', { name: 'Kumaş girişini kaydet' }));
    await screen.findByText(/Süprem · 1\.900 kg/);
    await userEvent.click(screen.getByRole('link', { name: 'Faturalanacaklar' }));
    const card = await screen.findByRole('article', { name: 'SVK-2026-0003' });
    expect(within(card).getByText(/Hatalı kilo 3,2 kg/)).toBeInTheDocument();
    await userEvent.click(within(card).getByRole('link', { name: 'SVK-2026-0003' }));
    expect(router.state.location.pathname).toBe('/panel/sevkiyat/shp-0003');
  });
});
