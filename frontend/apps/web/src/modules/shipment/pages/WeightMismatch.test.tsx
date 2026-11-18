import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { SHIPMENTS_PATH } from '@iplik/api-client';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { captureTicket } from '@/test/ticket';
import { addBagsTotalling1972Point5 } from '@/test/bags';
import { renderApp } from '@/test/renderApp';

const MISMATCH = '/panel/sevkiyat/shp-0045';

const LONG_FLOW_TIMEOUT_MS = 20_000;

describe('Kantar uyuşmazlığı', () => {
  it('müdürün ana sayfasında karar bekleyen sevkiyatı bildirir ve dosyaya bağlar', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel');
    const notice = await screen.findByTestId('pending-weight-decisions');
    expect(within(notice).getByText(/1 sevkiyat kararınızı bekliyor/)).toBeInTheDocument();
    expect(within(notice).getByRole('link', { name: 'SVK-2026-0045' })).toHaveAttribute(
      'href',
      MISMATCH,
    );
  });

  it('sevkiyat dosyasında beklenen, tartılan ve farkı gösterir', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp(MISMATCH);
    const panel = await screen.findByTestId('weight-check');
    expect(within(panel).getByText('1.982,5 kg')).toBeInTheDocument();
    expect(within(panel).getByText('1.970 kg')).toBeInTheDocument();
    expect(within(panel).getByText('-12,5 kg')).toBeInTheDocument();
    expect(within(panel).getByText(/Stok yetmedi/)).toBeInTheDocument();
    expect(
      screen.getByText('Kantar uyuşmazlığı', { selector: '.mantine-Badge-label' }),
    ).toBeInTheDocument();
  });

  it('dosyada reçete satırlarını ve fire oranını gösterir', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp(MISMATCH);
    const recipe = await screen.findByTestId('recipe-panel');
    expect(within(recipe).getByText('36/1 Viskon')).toBeInTheDocument();
    expect(within(recipe).getByText('20 denye likra')).toBeInTheDocument();
    expect(within(recipe).getByText(/Fire oranı: %4/)).toBeInTheDocument();
  });

  it(
    '"İplik güncelle" reçete sayfasına götürür; kilolar tutmadan kaydedilemez, tutunca sevkiyat yola çıkar',
    async () => {
      server.use(...createHandlers('MANAGER'));
      renderApp(MISMATCH);
      await userEvent.click(await screen.findByRole('radio', { name: /İplik güncelle/ }));
      await userEvent.click(screen.getByRole('link', { name: 'İplik listesini düzelt' }));
      const status = await screen.findByTestId('recipe-weight-status');
      expect(status).toHaveTextContent('Kilolar henüz tutmuyor');
      const submit = screen.getByRole('button', { name: 'Kaydet ve yola çıkar' });
      await userEvent.type(screen.getByLabelText(/Gerekçe/), 'Reçete düzeltildi');
      const secondKg = screen.getByLabelText('Net kg 2');
      await userEvent.clear(secondKg);
      await userEvent.type(secondKg, '470');
      expect(await screen.findByTestId('recipe-weight-status')).toHaveTextContent(
        'Kilolar tuttu, sevkiyat yola çıkabilir',
      );
      await userEvent.click(submit);
      expect(
        await screen.findByText('Fasoncuya yolda', { selector: '.mantine-Badge-label' }),
      ).toBeInTheDocument();
    },
    LONG_FLOW_TIMEOUT_MS,
  );

  it('kilolar tartıyı tutmuyorsa sunucu reddeder ve sevkiyat uyuşmazlıkta kalır', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp(`${MISMATCH}/recete`);
    await screen.findByTestId('recipe-weight-status');
    await userEvent.type(screen.getByLabelText(/Gerekçe/), 'Deneme');
    const secondKg = screen.getByLabelText('Net kg 2');
    await userEvent.clear(secondKg);
    await userEvent.type(secondKg, '470');
    await userEvent.clear(secondKg);
    await userEvent.type(secondKg, '469,5');
    expect(screen.getByTestId('recipe-weight-status')).toHaveTextContent('Kilolar henüz tutmuyor');
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet ve yola çıkar' }));
    expect(await screen.findByTestId('recipe-error')).toHaveTextContent('Kilolar tartıyı tutmuyor');
    expect(screen.queryByText('Fasoncuya yolda')).not.toBeInTheDocument();
  });

  it('karar paneli üç seçeneği sunar', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp(MISMATCH);
    const panel = await screen.findByTestId('weight-decision');
    ['Sevkiyatı iptal et', 'Çuval ekle / çıkar', 'İplik güncelle'].forEach((name) => {
      expect(within(panel).getByText(name)).toBeInTheDocument();
    });
  });

  it('depo karar paneli görmez ve çıkış tartısı giremez', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp(MISMATCH);
    await screen.findByTestId('weight-check');
    expect(screen.queryByTestId('weight-decision')).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Çıkış tartısını gir' })).not.toBeInTheDocument();
  });

  it('müdür uyuşmazlıktaki sevkiyatı iptal edebilir', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp(MISMATCH);
    await userEvent.click(await screen.findByRole('radio', { name: /Sevkiyatı iptal et/ }));
    await userEvent.type(screen.getByLabelText(/Gerekçe/), 'Araç döndü');
    await userEvent.click(screen.getByRole('button', { name: 'Kararı kaydet' }));
    expect(
      await screen.findByText('İptal', { selector: '.mantine-Badge-label' }),
    ).toBeInTheDocument();
  });

  it('depo beklenenden farklı tartı girince sevkiyat kantar uyuşmazlığına düşer', async () => {
    addBagsTotalling1972Point5();
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat/shp-0001/cikis');
    await screen.findByRole('button', { name: 'İleri' });
    await userEvent.type(screen.getByLabelText('Dolu araç tartısı (brüt)'), '1.990');
    await userEvent.type(screen.getByLabelText('Boş araç tartısı (dara)'), '17,5');
    await userEvent.type(screen.getByLabelText('Çuval sayısı'), '40');
    await userEvent.type(screen.getByLabelText(/Eksik kilo nedeni/), 'Stok yetmedi');
    await captureTicket();
    expect(
      await screen.findByText('Fotoğraf seçildi. Çıkış tartısını kaydedince sisteme yüklenecek.'),
    ).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'İleri' }));
    expect(await screen.findByText(/müdüre kantar uyuşmazlığı açılır/)).toBeInTheDocument();
    await userEvent.click(await screen.findByRole('button', { name: /^Onayla/ }));
    expect(
      await screen.findByText('Kantar uyuşmazlığı', { selector: '.mantine-Badge-label' }),
    ).toBeInTheDocument();
    expect(await screen.findByTestId('weight-check')).toHaveTextContent('Stok yetmedi');
  });

  it('liste süzgecinde Kantar uyuşmazlığı seçeneği vardır', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat?durum=WEIGHT_MISMATCH');
    expect(await screen.findByRole('link', { name: 'SVK-2026-0045' })).toBeInTheDocument();
  });

  it('müdür çuval ekle-çıkar derse sevkiyat dosyasında müdürün isteği ve notu görünür', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp(MISMATCH);
    await userEvent.click(await screen.findByRole('radio', { name: /Çuval ekle/ }));
    await userEvent.type(screen.getByLabelText(/Gerekçe/), 'Eksik çuvalı ekleyin');
    await userEvent.click(screen.getByRole('button', { name: 'Kararı kaydet' }));
    const note = await screen.findByTestId('manager-decision-note');
    expect(within(note).getByText(/Yalnızca bunu yapın/)).toBeInTheDocument();
    expect(within(note).getByText(/Eksik çuvalı ekleyin/)).toBeInTheDocument();
  });

  it('eksik kilo nedeni yazılmadan ileri denirse neden istenir ve hiçbir şey kaydedilmez', async () => {
    addBagsTotalling1972Point5();
    server.use(
      http.post(`${SHIPMENTS_PATH}/:shipmentId/dispatch-weighing`, () =>
        HttpResponse.json(
          { status: 400, code: 'SHORTFALL_REASON_REQUIRED', title: 'Eksik kilonun nedeni gerekli' },
          { status: 400 },
        ),
      ),
      ...createHandlers('WAREHOUSE'),
    );
    renderApp('/panel/sevkiyat/shp-0001/cikis');
    await screen.findByRole('button', { name: 'İleri' });
    await userEvent.type(screen.getByLabelText('Dolu araç tartısı (brüt)'), '1.990');
    await userEvent.type(screen.getByLabelText('Boş araç tartısı (dara)'), '17,5');
    await userEvent.type(screen.getByLabelText('Çuval sayısı'), '40');
    await captureTicket();
    await userEvent.click(screen.getByRole('button', { name: 'İleri' }));
    expect(
      await screen.findByText(
        'Tartılan net kilo beklenenin altında. Eksik kilonun nedenini yazın.',
      ),
    ).toBeInTheDocument();
  });
});
