import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { captureTicket } from '@/test/ticket';
import { renderApp } from '@/test/renderApp';

const MISMATCH = '/panel/sevkiyat/shp-0045';

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

  it('müdür gerekçesiz karar veremez, gerekçeyle reçeteyi güncelleyince sevkiyat yola çıkar', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp(MISMATCH);
    const submit = await screen.findByRole('button', { name: 'Kararı kaydet' });
    expect(submit).toBeDisabled();
    await userEvent.click(screen.getByText('Reçeteyi güncelle'));
    await userEvent.type(screen.getByLabelText(/Gerekçe/), 'Fark kabul edildi');
    await userEvent.click(submit);
    expect(
      await screen.findByText('Fasoncuya yolda', { selector: '.mantine-Badge-label' }),
    ).toBeInTheDocument();
  });

  it('karar paneli üç seçeneği sunar', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp(MISMATCH);
    const panel = await screen.findByTestId('weight-decision');
    ['İptal et', 'Çuval ekle-çıkar', 'Reçeteyi güncelle'].forEach((name) => {
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
    await userEvent.click(await screen.findByRole('button', { name: 'İptal et' }));
    await userEvent.type(screen.getByLabelText('İptal gerekçesi'), 'Araç döndü');
    await userEvent.click(screen.getByRole('button', { name: 'Sevkiyatı iptal et' }));
    expect(
      await screen.findByText('İptal', { selector: '.mantine-Badge-label' }),
    ).toBeInTheDocument();
  });

  it('depo beklenenden farklı tartı girince sevkiyat kantar uyuşmazlığına düşer', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat/shp-0001/cikis');
    await screen.findByRole('button', { name: 'Kaydet' });
    await userEvent.type(screen.getByLabelText('Dolu araç tartısı (brüt)'), '1.990');
    await userEvent.type(screen.getByLabelText('Boş araç tartısı (dara)'), '17,5');
    await userEvent.type(screen.getByLabelText('Çuval sayısı'), '40');
    await userEvent.type(screen.getByLabelText(/Eksik gönderim nedeni/), 'Stok yetmedi');
    await captureTicket();
    expect(
      await screen.findByText('Kantar fişi fotoğrafı kaydedildi, devam edebilirsiniz.'),
    ).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }));
    expect(
      await screen.findByText('Kantar uyuşmazlığı', { selector: '.mantine-Badge-label' }),
    ).toBeInTheDocument();
    expect(await screen.findByTestId('weight-check')).toHaveTextContent('Stok yetmedi');
  });

  it('liste süzgecinde Kantar uyuşmazlığı seçeneği vardır', async () => {
    renderApp('/panel/sevkiyat?durum=WEIGHT_MISMATCH');
    expect(await screen.findByRole('link', { name: 'SVK-2026-0045' })).toBeInTheDocument();
  });
});
