import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { assignMockBagWeights } from '@/mocks/mockStore';
import { server } from '@/mocks/server';
import { captureTicket } from '@/test/ticket';
import { addBagsTotalling1982Point5 } from '@/test/bags';
import { renderApp } from '@/test/renderApp';

async function fill(values: Record<string, string>) {
  for (const [label, value] of Object.entries(values)) {
    await userEvent.type(screen.getByLabelText(label), value);
  }
}

describe('DispatchWeighingPage', () => {
  it('depo ana sayfasında yüklenecek sevkiyatları listeler', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel');
    const card = await screen.findByTestId('pending-dispatches');
    expect(card).toHaveTextContent(/Yüklenecek sevkiyat: \d+/);
    expect(card).toHaveTextContent('önce çuvalları ekleyin, sonra çıkış tartısını girin');
  });

  it('çuval eklenmemişse tartı düğmesi görünmez ve önce çuval eklenmesini söyler', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    server.use(
      http.get('/api/v1/shipments/shp-0001/bags', () =>
        HttpResponse.json({ dispatchBags: [], deliveryBags: [] }),
      ),
    );
    renderApp('/panel/sevkiyat/shp-0001');
    expect(
      await screen.findByText(
        'Önce "Çuvallar" sekmesinden çuvalları ekleyin, sonra tartıyı girin.',
      ),
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Çıkış tartısını gir' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Çıkış tartısını gir' })).not.toBeInTheDocument();
  });

  it('depo çıkış tartısını kaydeder, sevkiyat Fasoncuya yolda olur', async () => {
    addBagsTotalling1982Point5();
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat/shp-0001/cikis');
    await screen.findByLabelText('Dolu araç tartısı (brüt)');
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
    await userEvent.click(screen.getByRole('button', { name: 'İleri' }));
    await userEvent.click(await screen.findByRole('button', { name: /^Onayla/ }));
    expect(
      await screen.findByText('Fasoncuya yolda', { selector: '.mantine-Badge-label' }),
    ).toBeInTheDocument();
  });

  it('fotoğraf, çıkış tartısı kaydedilmeden sisteme yüklenmez; onaylanınca yüklenir', async () => {
    addBagsTotalling1982Point5();
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
    await screen.findByRole('button', { name: 'İleri' });
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
    await userEvent.click(screen.getByRole('button', { name: 'İleri' }));
    await userEvent.click(await screen.findByRole('button', { name: /^Onayla/ }));
    expect(
      await screen.findByText('Fasoncuya yolda', { selector: '.mantine-Badge-label' }),
    ).toBeInTheDocument();
    expect(uploads).toBe(1);
    server.events.removeAllListeners();
  });

  it('sunucu çıkış tartısını reddederse fotoğraf hiç yüklenmez', async () => {
    addBagsTotalling1982Point5();
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
    await screen.findByRole('button', { name: 'İleri' });
    await fill({
      'Dolu araç tartısı (brüt)': '2.000',
      'Boş araç tartısı (dara)': '17,5',
      'Çuval sayısı': '40',
    });
    await captureTicket();
    await userEvent.click(screen.getByRole('button', { name: 'İleri' }));
    await userEvent.click(await screen.findByRole('button', { name: /^Onayla/ }));
    expect(await screen.findByText(/Tartı değerleri tutarsız/)).toBeInTheDocument();
    expect(uploads).toBe(0);
    server.events.removeAllListeners();
  });

  it('hatalı girişte kaydetmeden uyarır', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat/shp-0001/cikis');
    await userEvent.click(await screen.findByRole('button', { name: 'İleri' }));
    expect(await screen.findByText('Kiloyu 1.982,5 biçiminde girin.')).toBeInTheDocument();
  });

  it('fiş çekilmeden kaydetmez', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat/shp-0001/cikis');
    await screen.findByRole('button', { name: 'İleri' });
    await fill({
      'Dolu araç tartısı (brüt)': '2.000',
      'Boş araç tartısı (dara)': '17',
      'Çuval sayısı': '40',
    });
    await userEvent.click(screen.getByRole('button', { name: 'İleri' }));
    expect(
      await screen.findByText('Devam etmek için kantar fişinin fotoğrafı gerekli.'),
    ).toBeInTheDocument();
  });

  it('çıkış yetkisi olmayan rol 403 sayfasına düşer', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat/shp-0001/cikis');
    expect(await screen.findByText('Bu sayfayı görme yetkiniz yok.')).toBeInTheDocument();
  });

  it('çuvalların toplamı araç net kilosunu tutmazsa uyarır ve çuvallar sekmesine götürür', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    assignMockBagWeights('shp-0001', ['50.000', '50.000']);
    renderApp('/panel/sevkiyat/shp-0001/cikis');
    await screen.findByRole('button', { name: 'İleri' });
    await fill({
      'Dolu araç tartısı (brüt)': '2.000',
      'Boş araç tartısı (dara)': '17,5',
      'Çuval sayısı': '2',
    });
    await captureTicket();
    await userEvent.click(screen.getByRole('button', { name: 'İleri' }));
    const dialog = await screen.findByRole('dialog', { name: 'Çuvallar araç kilosunu tutmuyor' });
    expect(dialog).toHaveTextContent('100 kg');
    expect(dialog).toHaveTextContent('1.982,5 kg');
    await userEvent.click(within(dialog).getByRole('link', { name: 'Çuvalları kontrol et' }));
    expect(
      await screen.findByRole('tab', { name: /Çuvallar/, selected: true }),
    ).toBeInTheDocument();
  });

  it('tartıya yazılan çuval sayısı eklenen çuvalları tutmazsa sayıyı söyler ve çuvallar sekmesine götürür', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    assignMockBagWeights('shp-0001', ['991.250', '991.250']);
    renderApp('/panel/sevkiyat/shp-0001/cikis');
    await screen.findByRole('button', { name: 'İleri' });
    await fill({
      'Dolu araç tartısı (brüt)': '2.000',
      'Boş araç tartısı (dara)': '17,5',
      'Çuval sayısı': '3',
    });
    await captureTicket();
    await userEvent.click(screen.getByRole('button', { name: 'İleri' }));
    const dialog = await screen.findByRole('dialog', { name: 'Çuval sayısı tutmuyor' });
    expect(dialog).toHaveTextContent('Tartıya 3 çuval yazdınız, ama sevkiyata 2 çuval eklenmiş.');
    await userEvent.click(within(dialog).getByRole('link', { name: 'Çuvalları kontrol et' }));
    expect(
      await screen.findByRole('tab', { name: /Çuvallar/, selected: true }),
    ).toBeInTheDocument();
  });

  it('eksik kilo nedeni yalnızca net kilo beklenenin altındayken sorulur', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat/shp-0001/cikis');
    await screen.findByRole('button', { name: 'İleri' });
    expect(screen.queryByLabelText(/Eksik kilo nedeni/)).not.toBeInTheDocument();
    await fill({ 'Dolu araç tartısı (brüt)': '1.990', 'Boş araç tartısı (dara)': '17,5' });
    expect(await screen.findByLabelText(/Eksik kilo nedeni/)).toBeInTheDocument();
  });
});
