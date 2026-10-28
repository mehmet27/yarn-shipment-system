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

  it('kayıttan sonra başarı bildirimi gösterir', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp('/panel/tanimlar');
    await userEvent.click(await screen.findByRole('button', { name: 'Ekle' }));
    await userEvent.type(screen.getByLabelText(/Fasoncu ünvanı/), 'Yeni Örme Ltd.');
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Kayıt eklendi.');

    const card = await screen.findByRole('group', { name: 'Yeni Örme Ltd.' });
    await userEvent.click(within(card).getByRole('button', { name: 'Düzenle' }));
    await userEvent.type(screen.getByLabelText(/Fasoncu ünvanı/), ' A.Ş.');
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Değişiklikler kaydedildi.');
  });

  it('aynı fasoncu ünvanını sunucu hatasıyla alanın altında gösterir', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp('/panel/tanimlar');
    await userEvent.click(await screen.findByRole('button', { name: 'Ekle' }));
    await userEvent.type(screen.getByLabelText(/Fasoncu ünvanı/), ' aydın örme ');
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }));
    expect(
      await screen.findByText('Bu ünvanla kayıtlı bir fasoncu zaten var.'),
    ).toBeInTheDocument();
    expect(screen.getByLabelText(/Fasoncu ünvanı/)).toHaveAttribute('aria-invalid', 'true');
  });

  it('TR plaka desenine uymayan plakayı göndermeden reddeder', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp('/panel/tanimlar');
    await userEvent.click(await screen.findByRole('tab', { name: 'Araçlar' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Ekle' }));
    await userEvent.type(screen.getByLabelText(/Plaka/), 'abc');
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }));
    expect(
      await screen.findByText('Geçerli bir plaka girin (ör. 34 ABC 123).'),
    ).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText(/Plaka/), 'x');
    expect(screen.queryByText('Geçerli bir plaka girin (ör. 34 ABC 123).')).not.toBeInTheDocument();
  });

  it('giriş hesabı formu eksik alanları düğmeye basınca gösterir', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp('/panel/tanimlar');
    const card = await screen.findByRole('group', { name: 'Aydın Örme' });
    await userEvent.click(within(card).getByRole('button', { name: 'Giriş hesabı aç' }));
    const dialog = await screen.findByRole('dialog');
    const submit = within(dialog).getByRole('button', { name: 'Hesabı aç' });
    expect(submit).toBeEnabled();
    expect(within(dialog).getByText(/Görebileceği lokasyonlar/)).toHaveTextContent(
      'Görebileceği lokasyonlar *',
    );

    await userEvent.click(submit);
    expect(within(dialog).getByText(/Giriş adı en az 3 karakter olmalı/)).toBeInTheDocument();
    expect(within(dialog).getByText(/Geçerli bir e-posta girin/)).toBeInTheDocument();
    expect(within(dialog).getByText('En az bir lokasyon seçin.')).toBeInTheDocument();

    await userEvent.type(within(dialog).getByLabelText(/Giriş adı/), 'şahin');
    await userEvent.type(within(dialog).getByLabelText(/E-posta/), 'aydin@orme.com');
    await userEvent.click(await within(dialog).findByRole('switch', { name: 'Merkez Depo' }));
    await userEvent.click(submit);
    expect(within(dialog).getByText(/Giriş adı en az 3 karakter olmalı/)).toBeInTheDocument();
    expect(within(dialog).queryByText(/Geçerli bir e-posta girin/)).not.toBeInTheDocument();
    expect(within(dialog).queryByText('En az bir lokasyon seçin.')).not.toBeInTheDocument();

    await userEvent.clear(within(dialog).getByLabelText(/Giriş adı/));
    await userEvent.type(within(dialog).getByLabelText(/Giriş adı/), 'aydin.orme');
    await userEvent.click(submit);
    expect(await within(dialog).findByText('Giriş hesabı açıldı.')).toBeInTheDocument();
  });

  it('MASTER_DATA_MANAGE yetkisi olmayan rol bu sayfaya giremez', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/tanimlar');
    expect(await screen.findByText(/yetki/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Ekle' })).not.toBeInTheDocument();
  });
});
