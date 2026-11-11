import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

async function pick(label: string, option: string) {
  await userEvent.click(await screen.findByRole('combobox', { name: new RegExp(label) }));
  await userEvent.click(await screen.findByRole('option', { name: option }));
}

async function fillRecipe(netKg: string) {
  await userEvent.type(await screen.findByLabelText(/ya da serbest metin/), '20 denye likra');
  await userEvent.type(screen.getByLabelText(/Net kg 1/), netKg);
}

const LONG_FLOW_TIMEOUT_MS = 20_000;

describe('CreateShipmentPage', () => {
  it(
    'müdür taslak sevkiyat oluşturur ve dosyasına gider',
    async () => {
      server.use(...createHandlers('MANAGER'));
      renderApp('/panel/sevkiyat');
      await userEvent.click(await screen.findByRole('link', { name: 'Yeni sevkiyat' }));
      await pick('Lokasyon', 'Merkez Depo');
      await pick('Fasoncu', 'Bursa Tekstil');
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

  it('fasoncu seçilmeden göndermeyi engeller', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat/yeni');
    await pick('Lokasyon', 'Merkez Depo');
    await userEvent.click(screen.getByRole('button', { name: 'Taslak oluştur' }));
    expect(await screen.findByText('Fasoncu seçin.')).toBeInTheDocument();
  });

  it('reçete satırı eklenip silinir, en az bir satır kalır ve toplam canlı güncellenir', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat/yeni');
    await fillRecipe('1.000');
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
    await pick('Lokasyon', 'Merkez Depo');
    await pick('Fasoncu', 'Bursa Tekstil');
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
    await pick('Fasoncu', 'Aydın Örme');
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
