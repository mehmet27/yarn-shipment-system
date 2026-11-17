import { getShipment } from '@iplik/api-client';
import { formatDate, todayIsoDate } from '@iplik/i18n';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { addMockShipment } from '@/mocks/mockStore';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

describe('ShipmentFilePage', () => {
  beforeEach(() => {
    server.use(...createHandlers('MANAGER'));
  });

  it('tamamlanan sevkiyatta çıkış tartısını ve irsaliye numarasını gösterir', async () => {
    renderApp('/panel/sevkiyat/shp-0007');
    expect(await screen.findByRole('heading', { name: 'SVK-2026-0007' })).toBeInTheDocument();
    const netRow = screen.getByRole('row', { name: /^Net/ });
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

describe('sevkiyat günü', () => {
  const futureShipment = () =>
    addMockShipment({
      siteId: 'site-merkez',
      subcontractorId: 'sub-0',
      subcontractorTitle: 'Örnek Tekstil A.Ş.',
      lines: [{ yarnTypeText: '30/1 Penye', netKg: '100.000' }],
      fireRate: '0.0400',
      shipmentDate: '2099-12-20',
    });

  it('depo, sevkiyat günü gelmeden çıkış tartısı düğmesini görmez ve tarihi okur', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp(`/panel/sevkiyat/${futureShipment().shipmentId}`);
    expect(await screen.findByText(/Sevkiyat tarihi 20.12.2099./)).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Çıkış tartısını gir' })).not.toBeInTheDocument();
  });

  it('depo, sevkiyat günü gelmeden çıkış fotoğrafı bölümünü görmez', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp(`/panel/sevkiyat/${futureShipment().shipmentId}`);
    await screen.findByText(/Sevkiyat tarihi 20.12.2099./);
    expect(screen.queryByRole('button', { name: 'Fotoğraf çek' })).not.toBeInTheDocument();
  });

  it('sevkiyat günü gelmiş sevkiyatta depo çıkış tartısı düğmesini görür', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat/shp-0001');
    expect(await screen.findByRole('link', { name: 'Çıkış tartısını gir' })).toBeInTheDocument();
    expect(screen.queryByText(/Bu tarihten önce çuval atanamaz/)).not.toBeInTheDocument();
  });
});

describe('sevkiyat günü güncelleme', () => {
  it('müdür taslakta tarihi değiştirir; depo uyarısı kalkar ve çıkış düğmesi gelir', async () => {
    server.use(...createHandlers('MANAGER'));
    const created = addMockShipment({
      siteId: 'site-merkez',
      subcontractorId: 'sub-0',
      subcontractorTitle: 'Örnek Tekstil A.Ş.',
      lines: [{ yarnTypeText: '30/1 Penye', netKg: '100.000' }],
      fireRate: '0.0400',
      shipmentDate: '2099-12-20',
    });
    renderApp(`/panel/sevkiyat/${created.shipmentId}`);
    await userEvent.click(await screen.findByRole('button', { name: 'Tarihi değiştir' }));
    const date = screen.getByLabelText(/Yeni sevkiyat tarihi/);
    await userEvent.clear(date);
    await userEvent.type(date, todayIsoDate());
    await userEvent.click(screen.getByRole('button', { name: 'Tarihi güncelle' }));
    await waitFor(() => {
      expect(screen.queryByText(/Bu tarihten önce çuval atanamaz/)).not.toBeInTheDocument();
    });
    expect(await screen.findByText(formatDate(todayIsoDate()))).toBeInTheDocument();
  });

  it('depo tarihi değiştiremez; tamamlanmış sevkiyatta da düğme yoktur', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat/shp-0001');
    await screen.findByRole('heading', { name: 'SVK-2026-0001' });
    expect(screen.queryByRole('button', { name: 'Tarihi değiştir' })).not.toBeInTheDocument();
  });
});

describe('sahte API görünüm kuralı', () => {
  it('depoya depo görünümünü çıkış tartısıyla verir', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    const shipment = await getShipment('shp-0007');
    expect(shipment.audience).toBe('WAREHOUSE');
    expect(shipment).toHaveProperty('dispatchWeighing');
  });
});
