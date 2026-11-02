import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

/** shp-0007 tamamlanmış (kilitli) sevkiyattır; çıkış tartısı Brüt 2.000 kg, Dara 17,5 kg. */
const COMPLETED_FILE = '/panel/sevkiyat/shp-0007';

describe('Gerekçeli düzeltme', () => {
  it('Admin tamamlanmış sevkiyatın çıkış tartısını gerekçeyle düzeltir; eski değer listede kalır', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp(COMPLETED_FILE);
    await userEvent.click(await screen.findByRole('tab', { name: 'Düzeltmeler' }));
    expect(await screen.findByText('Bu sevkiyatta düzeltme yok.')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('combobox', { name: /^Ne düzeltilecek/ }));
    await userEvent.click(await screen.findByRole('option', { name: 'Çıkış tartısı' }));
    expect(screen.getByLabelText('Brüt')).toHaveValue('2000');
    await userEvent.clear(screen.getByLabelText('Brüt'));
    await userEvent.type(screen.getByLabelText('Brüt'), '2010');
    await userEvent.type(screen.getByLabelText(/^Gerekçe/), 'Kantar fişi yanlış okunmuş');
    await userEvent.click(screen.getByRole('button', { name: 'Düzeltmeyi kaydet' }));

    expect(await screen.findByText('Kantar fişi yanlış okunmuş')).toBeInTheDocument();
    expect(
      screen.getByText(
        'Brüt 2.000 kg · Dara 17,5 kg · Net 1.982,5 kg → Brüt 2.010 kg · Dara 17,5 kg · Net 1.992,5 kg',
      ),
    ).toBeInTheDocument();
  });

  it('gerekçesiz düzeltme gönderilmez', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp(COMPLETED_FILE);
    await userEvent.click(await screen.findByRole('tab', { name: 'Düzeltmeler' }));
    await userEvent.click(await screen.findByRole('combobox', { name: /^Ne düzeltilecek/ }));
    await userEvent.click(await screen.findByRole('option', { name: 'Çıkış mühür numarası' }));
    await userEvent.clear(screen.getByLabelText('Doğru mühür numarası'));
    await userEvent.type(screen.getByLabelText('Doğru mühür numarası'), 'M-9999');
    await userEvent.click(screen.getByRole('button', { name: 'Düzeltmeyi kaydet' }));

    expect(await screen.findByText('Gerekçe yazın.')).toBeInTheDocument();
    expect(screen.getByText('Bu sevkiyatta düzeltme yok.')).toBeInTheDocument();
  });

  it('yönetici düzeltmeleri görür ama formu görmez', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp(COMPLETED_FILE);
    await userEvent.click(await screen.findByRole('tab', { name: 'Düzeltmeler' }));
    expect(await screen.findByText('Bu sevkiyatta düzeltme yok.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Düzeltmeyi kaydet' })).not.toBeInTheDocument();
  });

  it('depo düzeltme sekmesini görmez', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat/shp-0003');
    expect(await screen.findByRole('tab', { name: 'Geçmiş' })).toBeInTheDocument();
    expect(screen.queryByRole('tab', { name: 'Düzeltmeler' })).not.toBeInTheDocument();
  });
});
