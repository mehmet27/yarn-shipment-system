import { screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';

describe('ExitSlipPage', () => {
  it('çuvalları sıra no ve kg olarak toplamıyla gösterir', async () => {
    renderApp('/panel/sevkiyat/shp-0007/ambar-cikis-fisi');
    expect(await screen.findByRole('heading', { name: 'Ambar Çıkış Fişi' })).toBeInTheDocument();
    expect(screen.getByText(/SVK-2026-0007/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Yazdır' })).toBeInTheDocument();
    const table = await screen.findByTestId('exit-slip-bags');
    expect(within(table).getByText(/Toplam \(\d+ çuval\)/)).toBeInTheDocument();
  });

  it('sevkiyat dosyasından fişe bağlantı verir', async () => {
    renderApp('/panel/sevkiyat/shp-0007');
    const link = await screen.findByRole('link', { name: 'Ambar çıkış fişi' });
    expect(link).toHaveAttribute('href', '/panel/sevkiyat/shp-0007/ambar-cikis-fisi');
  });
});
