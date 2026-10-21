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

describe('CreateShipmentPage', () => {
  it('depo kullanıcısı taslak sevkiyat oluşturur ve dosyasına gider', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat');
    await userEvent.click(await screen.findByRole('link', { name: 'Yeni sevkiyat' }));
    await pick('Lokasyon', 'Merkez Depo');
    await pick('Fasoncu', 'Bursa Tekstil');
    await pick('Şoför', 'Mert Kaya');
    await userEvent.click(screen.getByRole('button', { name: 'Taslak oluştur' }));
    expect(await screen.findByText('SVK-2026-0046')).toBeInTheDocument();
    expect(screen.getByText('Taslak')).toBeInTheDocument();
  });

  it('fasoncu seçilmeden göndermeyi engeller', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat/yeni');
    await pick('Lokasyon', 'Merkez Depo');
    await userEvent.click(screen.getByRole('button', { name: 'Taslak oluştur' }));
    expect(await screen.findByText('Fasoncu seçin.')).toBeInTheDocument();
  });

  it('şoför listesi seçilen fasoncuya göre daralır', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat/yeni');
    await pick('Fasoncu', 'Aydın Örme');
    await userEvent.click(await screen.findByRole('combobox', { name: /Şoför/ }));
    expect(await screen.findByRole('option', { name: 'Şahin Şoför' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'Mert Kaya' })).not.toBeInTheDocument();
  });

  it('SHIPMENT_CREATE yetkisi olmayan rolde buton görünmez', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat');
    await screen.findByText('SVK-2026-0001');
    expect(screen.queryByRole('link', { name: 'Yeni sevkiyat' })).not.toBeInTheDocument();
  });
});
