import { getShipment } from '@iplik/api-client';
import { screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

describe('ShipmentFilePage', () => {
  it('tamamlanan sevkiyatta çıkış tartısını ve irsaliye numarasını gösterir', async () => {
    renderApp('/panel/sevkiyat/shp-0007');
    expect(await screen.findByRole('heading', { name: 'SVK-2026-0007' })).toBeInTheDocument();
    const netRow = screen.getByRole('row', { name: /Net/ });
    expect(within(netRow).getByText('1.982,5 kg')).toBeInTheDocument();
    expect(screen.getByText('IRS-0007')).toBeInTheDocument();
  });

  it('çıkış tartısı girilmemiş taslakta bunu belirtir', async () => {
    renderApp('/panel/sevkiyat/shp-0001');
    await screen.findByRole('heading', { name: 'SVK-2026-0001' });
    expect(screen.getByText('Çıkış tartısı henüz girilmedi')).toBeInTheDocument();
  });

  it('olmayan sevkiyatta bulunamadı uyarısı gösterir', async () => {
    renderApp('/panel/sevkiyat/yok');
    expect(await screen.findByRole('alert')).toHaveTextContent('Aradığınız sayfa bulunamadı.');
  });
});

describe('sahte API görünüm kuralı', () => {
  it('fasoncuya çıkış tartısını verir, beklenen yükü ve irsaliyeyi vermez', async () => {
    server.use(...createHandlers('SUBCONTRACTOR'));
    const shipment = await getShipment('shp-0007');
    expect(shipment.audience).toBe('SUBCONTRACTOR');
    expect(shipment).toHaveProperty('dispatchWeighing');
    expect(shipment).not.toHaveProperty('waybillNo');
    expect(shipment).not.toHaveProperty('plannedNetKg');
  });

  it('depoya depo görünümünü çıkış tartısıyla verir', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    const shipment = await getShipment('shp-0007');
    expect(shipment.audience).toBe('WAREHOUSE');
    expect(shipment).toHaveProperty('dispatchWeighing');
  });
});
