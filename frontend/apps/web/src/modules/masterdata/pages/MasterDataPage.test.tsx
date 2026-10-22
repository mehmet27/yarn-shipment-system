import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

describe('MasterDataPage', () => {
  it('yönetici fasoncu ekler ve listede görür', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp('/panel/tanimlar');
    await userEvent.click(await screen.findByRole('button', { name: 'Ekle' }));
    await userEvent.type(screen.getByLabelText(/Fasoncu ünvanı/), 'Yeni Örme Ltd.');
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }));
    expect(await screen.findByText('Yeni Örme Ltd.')).toBeInTheDocument();
  });

  it('boş ünvanı göndermeden uyarır', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp('/panel/tanimlar');
    await userEvent.click(await screen.findByRole('button', { name: 'Ekle' }));
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }));
    expect(await screen.findByText('Bu alan boş olamaz.')).toBeInTheDocument();
  });

  it('pasife alınan kayıt listeden düşer, pasifleri göster ile geri açılır', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp('/panel/tanimlar');
    const card = await screen.findByRole('group', { name: 'Çorlu Boya' });
    await userEvent.click(within(card).getByRole('button', { name: 'Pasife al' }));
    await userEvent.click(await screen.findByRole('switch', { name: 'Pasifleri göster' }));
    expect(await screen.findByText('Eski Dokuma')).toBeInTheDocument();
    expect(screen.getByText('Çorlu Boya')).toBeInTheDocument();
    expect((await screen.findAllByText('Pasif')).length).toBeGreaterThan(1);
  });

  it('aynı plaka 409 ile reddedilir', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp('/panel/tanimlar');
    await userEvent.click(await screen.findByRole('tab', { name: 'Araçlar' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Ekle' }));
    await userEvent.type(screen.getByLabelText(/Plaka/), '34 abc 123');
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }));
    expect(await screen.findByText('Bu plaka başka bir araçta kullanılıyor.')).toBeInTheDocument();
  });

  it('MASTER_DATA_MANAGE yetkisi olmayan rol bu sayfaya giremez', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/tanimlar');
    expect(await screen.findByText(/yetki/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Ekle' })).not.toBeInTheDocument();
  });
});
