import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

describe('Sevkiyat dosyası sekmeleri', () => {
  it('Geçmiş sekmesi durum geçişlerini eskiden yeniye listeler', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp('/panel/sevkiyat/shp-0003');
    await userEvent.click(await screen.findByRole('tab', { name: 'Geçmiş' }));
    expect(await screen.findByText('Taslak → Yüklendi')).toBeInTheDocument();
  });

  it('depo çuval sekmesinde yalnızca çıkış listesini görür', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat/shp-0003');
    await userEvent.click(await screen.findByRole('tab', { name: 'Çuvallar' }));
    expect(await screen.findByText(/Çıkıştaki çuvallar/)).toBeInTheDocument();
    expect(screen.queryByText(/Teslimde okutulanlar/)).not.toBeInTheDocument();
  });
});
