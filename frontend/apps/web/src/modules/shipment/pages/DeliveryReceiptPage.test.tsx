import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { captureTicket } from '@/test/ticket';
import { renderApp } from '@/test/renderApp';

const next = () => userEvent.click(screen.getByRole('button', { name: 'İleri' }));

describe('DeliveryReceiptPage', () => {
  it('fasoncu adımları doldurup tutanağı gönderir, durum Kontrolde olur', async () => {
    server.use(...createHandlers('SUBCONTRACTOR'));
    renderApp('/saha/sevkiyat/shp-0004');
    await userEvent.click(await screen.findByRole('link', { name: 'Teslim tutanağını doldur' }));

    await next();
    expect(screen.getByText('Mühür durumunu seçin.')).toBeInTheDocument();
    await userEvent.click(screen.getByText('Sağlam'));
    await userEvent.type(screen.getByLabelText('Mühür numarası'), 'M-0004');
    await next();

    await userEvent.type(screen.getByLabelText('Çuval barkodu'), 'B-1{Enter}B-1{Enter}B-2{Enter}');
    expect(screen.getByText('Okutulan çuval: 2')).toBeInTheDocument();
    await next();

    await userEvent.type(screen.getByLabelText('Brüt (kg)'), '1.995,25');
    await userEvent.type(screen.getByLabelText('Dara (kg)'), '17');
    await userEvent.type(screen.getByLabelText('Tartılan çuval sayısı'), '2');
    expect(screen.getByText('Net: 1.978,25 kg')).toBeInTheDocument();
    await captureTicket();
    expect(await screen.findByText('Fiş okundu')).toBeInTheDocument();
    await next();
    await next();

    await userEvent.click(screen.getByRole('button', { name: 'Tutanağı gönder' }));
    expect(await screen.findByText('Kontrolde')).toBeInTheDocument();
  });

  it('kör giriş: çıkış tartısı ve mühür numarası hiçbir adımda görünmez', async () => {
    server.use(...createHandlers('SUBCONTRACTOR'));
    renderApp('/saha/sevkiyat/shp-0004/teslim');
    expect(await screen.findByText(/Teslim tutanağı/)).toBeInTheDocument();
    expect(screen.queryByText('M-0004')).not.toBeInTheDocument();
    expect(screen.queryByText(/2\.000|1\.982/)).not.toBeInTheDocument();
  });

  it('teslim beklemeyen sevkiyatta sihirbazı açmaz', async () => {
    server.use(...createHandlers('SUBCONTRACTOR'));
    renderApp('/saha/sevkiyat/shp-0007/teslim');
    expect(
      await screen.findByText('Bu sevkiyat artık teslim tutanağı beklemiyor.'),
    ).toBeInTheDocument();
  });
});
