import { getShipment } from '@iplik/api-client';
import { screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

describe('ShipmentFilePage', () => {
  it('çıkış ve teslim kilolarını farkıyla birlikte gösterir', async () => {
    renderApp('/panel/sevkiyat/shp-0007');
    expect(await screen.findByRole('heading', { name: 'SVK-2026-0007' })).toBeInTheDocument();
    const netRow = screen.getByRole('row', { name: /Net/ });
    expect(within(netRow).getByText('1.982,5 kg')).toBeInTheDocument();
    expect(within(netRow).getByText('1.968 kg')).toBeInTheDocument();
    expect(screen.getByText('%-0,73')).toBeInTheDocument();
  });

  it('teslim tartımı yoksa farkı hesaplamaz', async () => {
    renderApp('/panel/sevkiyat/shp-0002');
    await screen.findByRole('heading', { name: 'SVK-2026-0002' });
    const differenceRow = screen.getByRole('row', { name: /Fark/ });
    expect(within(differenceRow).getByText('Kaydedilmedi')).toBeInTheDocument();
  });

  it('olmayan sevkiyatta bulunamadı uyarısı gösterir', async () => {
    renderApp('/panel/sevkiyat/yok');
    expect(await screen.findByRole('alert')).toHaveTextContent('Aradığınız sayfa bulunamadı.');
  });
});

describe('sahte API kör giriş kuralı', () => {
  it('fasoncuya karşılaştırma bitmeden çıkış alanlarını hiç döndürmez', async () => {
    server.use(...createHandlers('FASONCU'));
    const shipment = await getShipment('shp-0004');
    expect(shipment.status).toBe('AWAITING_DELIVERY');
    expect(shipment).not.toHaveProperty('dispatchWeighing');
    expect(shipment).not.toHaveProperty('sealNo');
  });

  it('karşılaştırma bitince fasoncuya iki tarafı da döndürür', async () => {
    server.use(...createHandlers('FASONCU'));
    const shipment = await getShipment('shp-0007');
    expect(shipment.status).toBe('COMPLETED');
    expect(shipment.dispatchWeighing?.netKg).toBe('1982.500');
  });
});
