import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { http, HttpResponse } from 'msw';
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
  it('depo çıkış tartısını ve mührü kaydeder, sevkiyat Yüklendi olur', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat/shp-0001');
    await userEvent.click(
      await screen
        .findByRole('link', { name: 'Çıkış tartısını gir' })
        .catch(() => screen.findByRole('button', { name: 'Çıkış tartısını gir' })),
    );
    await fill({
      'Brüt (kg)': '2.000',
      'Dara (kg)': '17,5',
      'Çuval sayısı': '40',
      'Mühür numarası': 'M-NEW',
    });
    expect(screen.getByText('Net: 1.982,5 kg')).toBeInTheDocument();
    await captureTicket();
    expect(await screen.findByText('Fiş okundu')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }));
    expect(await screen.findByText('Yüklendi')).toBeInTheDocument();
  });

  it('hatalı girişte kaydetmeden uyarır', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat/shp-0001/cikis');
    await userEvent.click(await screen.findByRole('button', { name: 'Kaydet' }));
    expect(await screen.findByText('Kiloyu 1.982,5 biçiminde girin.')).toBeInTheDocument();
  });

  it('kullanılmış mühürde 409 mesajı gösterir', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat/shp-0001/cikis');
    await screen.findByRole('button', { name: 'Kaydet' });
    await fill({
      'Brüt (kg)': '2.000',
      'Dara (kg)': '17',
      'Çuval sayısı': '40',
      'Mühür numarası': 'M-0002',
    });
    await captureTicket();
    await screen.findByText('Fiş okundu');
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }));
    expect(
      await screen.findByText('Bu mühür numarası başka bir sevkiyatta kullanılmış.'),
    ).toBeInTheDocument();
  });

  it('fiş çekilmeden kaydetmez', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat/shp-0001/cikis');
    await screen.findByRole('button', { name: 'Kaydet' });
    await fill({
      'Brüt (kg)': '2.000',
      'Dara (kg)': '17',
      'Çuval sayısı': '40',
      'Mühür numarası': 'M-NEW',
    });
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }));
    expect(
      await screen.findByText('Devam etmek için kantar fişinin fotoğrafı gerekli.'),
    ).toBeInTheDocument();
  });

  it('okunamayan fişi üç kez dener, sonra gerekçeyle devam ettirir', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat/shp-0001/cikis');
    await screen.findByRole('button', { name: 'Kaydet' });
    await fill({ 'Brüt (kg)': '2.000', 'Dara (kg)': '17', 'Çuval sayısı': '40' });
    for (let attempt = 0; attempt < 3; attempt++) {
      await captureTicket(false);
      await screen.findByText('Kilo bilgisi okunamadı, lütfen tekrar çekin');
    }
    expect(await screen.findByText(/Üç denemede de okunamadı/)).toBeInTheDocument();
    await userEvent.click(await screen.findByRole('combobox', { name: 'Gerekçe' }));
    await userEvent.click(await screen.findByRole('option', { name: 'Yazıcı çalışmıyor' }));
    await userEvent.click(screen.getByRole('button', { name: 'Gerekçeyle devam et' }));
    expect(await screen.findByText(/K0 doğrulanamadı/)).toBeInTheDocument();
  });

  it('okuma servisi kapalıysa (503) okumayı atlayıp devam ettirir', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    server.use(
      http.post('/api/v1/evidence/:evidenceId/ticket-reading', () =>
        HttpResponse.json(
          { status: 503, code: 'TICKET_READING_UNAVAILABLE', title: 'x' },
          { status: 503 },
        ),
      ),
    );
    renderApp('/panel/sevkiyat/shp-0001/cikis');
    await screen.findByRole('button', { name: 'Kaydet' });
    await fill({ 'Brüt (kg)': '2.000', 'Dara (kg)': '17', 'Çuval sayısı': '40' });
    await captureTicket();
    expect(await screen.findByText(/okuma servisi şu an kapalı/)).toBeInTheDocument();
  });

  it('çıkış yetkisi olmayan rol 403 sayfasına düşer', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat/shp-0001/cikis');
    expect(await screen.findByText('Bu sayfayı görme yetkiniz yok.')).toBeInTheDocument();
  });
});
