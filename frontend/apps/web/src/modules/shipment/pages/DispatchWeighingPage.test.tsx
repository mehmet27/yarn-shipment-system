import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { captureTicket } from '@/test/ticket';
import { renderApp } from '@/test/renderApp';

async function fill(values: Record<string, string>) {
  for (const [label, value] of Object.entries(values)) {
    await userEvent.type(screen.getByLabelText(label), value);
  }
}

describe('DispatchWeighingPage', () => {
  it('depo çıkış tartısını kaydeder, sevkiyat Fasoncuya yolda olur', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat/shp-0001');
    await userEvent.click(
      await screen
        .findByRole('link', { name: 'Çıkış tartısını gir' })
        .catch(() => screen.findByRole('button', { name: 'Çıkış tartısını gir' })),
    );
    await fill({
      'Dolu araç tartısı (brüt)': '2.000',
      'Boş araç tartısı (dara)': '17,5',
      'Çuval sayısı': '40',
    });
    expect(screen.getByText('Net: 1.982,5 kg')).toBeInTheDocument();
    await captureTicket();
    expect(
      await screen.findByText('Fotoğraf seçildi. Çıkış tartısını kaydedince sisteme yüklenecek.'),
    ).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }));
    expect(
      await screen.findByText('Fasoncuya yolda', { selector: '.mantine-Badge-label' }),
    ).toBeInTheDocument();
  });

  it('fotoğraf, çıkış tartısı kaydedilmeden sisteme yüklenmez; Kaydet ile yüklenir', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    let uploads = 0;
    server.events.on('request:start', ({ request }) => {
      if (
        request.method === 'POST' &&
        new URL(request.url).pathname === '/api/v1/evidence/uploads'
      ) {
        uploads += 1;
      }
    });
    renderApp('/panel/sevkiyat/shp-0001/cikis');
    await screen.findByRole('button', { name: 'Kaydet' });
    await fill({
      'Dolu araç tartısı (brüt)': '2.000',
      'Boş araç tartısı (dara)': '17,5',
      'Çuval sayısı': '40',
    });
    await captureTicket();
    expect(
      await screen.findByText('Fotoğraf seçildi. Çıkış tartısını kaydedince sisteme yüklenecek.'),
    ).toBeInTheDocument();
    expect(uploads).toBe(0);
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }));
    expect(
      await screen.findByText('Fasoncuya yolda', { selector: '.mantine-Badge-label' }),
    ).toBeInTheDocument();
    expect(uploads).toBe(1);
    server.events.removeAllListeners();
  });

  it('sunucu çıkış tartısını reddederse fotoğraf hiç yüklenmez', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    server.use(
      http.post('/api/v1/shipments/:shipmentId/dispatch-weighing', () =>
        HttpResponse.json(
          { type: 'about:blank', title: 'x', status: 409, code: 'INVALID_DISPATCH' },
          { status: 409, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    );
    let uploads = 0;
    server.events.on('request:start', ({ request }) => {
      if (
        request.method === 'POST' &&
        new URL(request.url).pathname === '/api/v1/evidence/uploads'
      ) {
        uploads += 1;
      }
    });
    renderApp('/panel/sevkiyat/shp-0001/cikis');
    await screen.findByRole('button', { name: 'Kaydet' });
    await fill({
      'Dolu araç tartısı (brüt)': '2.000',
      'Boş araç tartısı (dara)': '17,5',
      'Çuval sayısı': '40',
    });
    await captureTicket();
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }));
    expect(await screen.findByText(/Tartı değerleri tutarsız/)).toBeInTheDocument();
    expect(uploads).toBe(0);
    server.events.removeAllListeners();
  });

  it('hatalı girişte kaydetmeden uyarır', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat/shp-0001/cikis');
    await userEvent.click(await screen.findByRole('button', { name: 'Kaydet' }));
    expect(await screen.findByText('Kiloyu 1.982,5 biçiminde girin.')).toBeInTheDocument();
  });

  it('fiş çekilmeden kaydetmez', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat/shp-0001/cikis');
    await screen.findByRole('button', { name: 'Kaydet' });
    await fill({
      'Dolu araç tartısı (brüt)': '2.000',
      'Boş araç tartısı (dara)': '17',
      'Çuval sayısı': '40',
    });
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }));
    expect(
      await screen.findByText('Devam etmek için kantar fişinin fotoğrafı gerekli.'),
    ).toBeInTheDocument();
  });

  it('çıkış yetkisi olmayan rol 403 sayfasına düşer', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat/shp-0001/cikis');
    expect(await screen.findByText('Bu sayfayı görme yetkiniz yok.')).toBeInTheDocument();
  });
});
