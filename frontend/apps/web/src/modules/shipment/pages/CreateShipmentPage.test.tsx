import { todayIsoDate } from '@iplik/i18n';
import { act, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

async function pick(label: string, option: string) {
  await userEvent.click(await screen.findByRole('combobox', { name: new RegExp(label) }));
  await userEvent.click(await screen.findByRole('option', { name: option }));
}

async function fillRecipe(netKg: string, withPrice = true) {
  await userEvent.type(await screen.findByLabelText(/ya da listede yoksa yazın/), '20 denye likra');
  await userEvent.type(screen.getByLabelText(/Net kg 1/), netKg);
  if (!withPrice) return;
  await userEvent.click(screen.getByRole('tab', { name: /Sipariş bilgileri/ }));
  await userEvent.type(screen.getByLabelText(/Birim fiyat/), '21,5');
}

const LONG_FLOW_TIMEOUT_MS = 20_000;

describe('CreateShipmentPage', () => {
  it(
    'müdür taslak sevkiyat oluşturur ve dosyasına gider',
    async () => {
      server.use(...createHandlers('MANAGER'));
      renderApp('/panel/sevkiyat');
      await userEvent.click(await screen.findByRole('link', { name: 'Yeni sevkiyat' }));
      await pick('Alıcı fasoncu', 'Bursa Tekstil');
      await pick('Şoför', 'Mert Kaya');
      await fillRecipe('2.036');
      expect(screen.getByLabelText(/Fire oranı/)).toHaveValue('4');
      expect(screen.getByTestId('recipe-total')).toHaveTextContent('2.036 kg');
      await userEvent.click(screen.getByRole('button', { name: 'Taslak oluştur' }));
      expect(await screen.findByText('SVK-2026-0046')).toBeInTheDocument();
      expect(screen.getByText('Taslak', { selector: '.mantine-Badge-label' })).toBeInTheDocument();
    },
    LONG_FLOW_TIMEOUT_MS,
  );

  it(
    'fiyat boşken kilo hatası olan satırda İplik sekmesi açık kalır',
    async () => {
      server.use(...createHandlers('MANAGER'));
      renderApp('/panel/sevkiyat/yeni');
      await pick('Alıcı fasoncu', 'Bursa Tekstil');
      await userEvent.type(
        await screen.findByLabelText(/ya da listede yoksa yazın/),
        '20 denye likra',
      );
      await userEvent.click(screen.getByRole('button', { name: 'Taslak oluştur' }));
      expect(await screen.findByLabelText(/Net kg 1/)).toBeVisible();
    },
    LONG_FLOW_TIMEOUT_MS,
  );

  it(
    'art arda iki tıklama aynı sipariş için tek sevkiyat açar',
    async () => {
      server.use(...createHandlers('MANAGER'));
      let posts = 0;
      server.events.on('request:start', ({ request }) => {
        if (request.method === 'POST' && new URL(request.url).pathname === '/api/v1/shipments') {
          posts += 1;
        }
      });
      renderApp('/panel/sevkiyat/yeni');
      await pick('Alıcı fasoncu', 'Bursa Tekstil');
      await fillRecipe('2.036');
      const button = await screen.findByRole('button', { name: 'Taslak oluştur' });
      act(() => {
        button.click();
        button.click();
      });
      expect(await screen.findByText('SVK-2026-0046')).toBeInTheDocument();
      expect(posts).toBe(1);
      server.events.removeAllListeners();
    },
    LONG_FLOW_TIMEOUT_MS,
  );

  it('sevkiyat tarihi bugünle gelir; silinirse gönderilmez ve tarih seçin uyarısı çıkar', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat/yeni');
    const date = await screen.findByLabelText(/Sevkiyat tarihi/);
    expect(date).toHaveValue(todayIsoDate());
    await userEvent.clear(date);
    await pick('Alıcı fasoncu', 'Bursa Tekstil');
    await fillRecipe('2.036');
    await userEvent.click(screen.getByRole('button', { name: 'Taslak oluştur' }));
    expect(await screen.findByText('Sevkiyat tarihini seçin.')).toBeInTheDocument();
  });

  it('fasoncu seçilmeden göndermeyi engeller', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat/yeni');
    await userEvent.click(await screen.findByRole('button', { name: 'Taslak oluştur' }));
    expect(await screen.findByText('Fasoncu seçin.')).toBeInTheDocument();
  });

  it('reçete satırı eklenip silinir, en az bir satır kalır ve toplam canlı güncellenir', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat/yeni');
    await fillRecipe('1.000', false);
    await userEvent.click(screen.getByRole('button', { name: 'Satır ekle' }));
    await userEvent.type(screen.getByLabelText(/Net kg 2/), '500,5');
    expect(screen.getByTestId('recipe-total')).toHaveTextContent('1.500,5 kg');
    await userEvent.click(screen.getByRole('button', { name: '2. satırı sil' }));
    expect(screen.queryByLabelText(/Net kg 2/)).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '1. satırı sil' })).not.toBeInTheDocument();
  });

  it('geçersiz reçete satırı ve fire oranını engeller', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat/yeni');
    await pick('Alıcı fasoncu', 'Bursa Tekstil');
    await userEvent.clear(screen.getByLabelText(/Fire oranı/));
    await userEvent.type(screen.getByLabelText(/Fire oranı/), '25');
    await userEvent.click(screen.getByRole('button', { name: 'Taslak oluştur' }));
    expect(
      await screen.findByText('Her satırda iplik cinsi seçin ya da yazın.'),
    ).toBeInTheDocument();
    expect(screen.getByText(/Fire oranı 0 ile 20 arasında/)).toBeInTheDocument();
  });

  it('şoför listesi seçilen fasoncuya göre daralır', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat/yeni');
    await pick('Alıcı fasoncu', 'Aydın Örme');
    await userEvent.click(await screen.findByRole('combobox', { name: /Şoför/ }));
    expect(await screen.findByRole('option', { name: 'Şahin Şoför' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'Mert Kaya' })).not.toBeInTheDocument();
  });

  it('depo yeni sevkiyat açamaz', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat');
    await screen.findByText('SVK-2026-0001');
    expect(screen.queryByRole('link', { name: 'Yeni sevkiyat' })).not.toBeInTheDocument();
  });

  it('SHIPMENT_CREATE yetkisi olmayan rolde buton görünmez', async () => {
    server.use(...createHandlers('QUALITY'));
    renderApp('/panel/sevkiyat');
    await screen.findByText('SVK-2026-0001');
    expect(screen.queryByRole('link', { name: 'Yeni sevkiyat' })).not.toBeInTheDocument();
  });
});
