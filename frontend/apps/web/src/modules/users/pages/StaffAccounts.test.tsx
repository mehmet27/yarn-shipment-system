import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

const PATH = '/api/v1/master-data/login-accounts';

async function pickRole(dialog: HTMLElement, optionName: string) {
  await userEvent.click(within(dialog).getByRole('combobox', { name: /Rol/ }));
  await userEvent.click(await screen.findByRole('option', { name: optionName }));
}

describe('Kullanıcılar: hesap açma ve rol değiştirme', () => {
  it('yönetici depo hesabı açar; şifre sorulmaz ve gösterilmez, hesap listede ilk giriş bekleyen olarak görünür', async () => {
    renderApp('/panel/kullanicilar');
    await userEvent.click(await screen.findByRole('button', { name: 'Yeni kullanıcı' }));
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).queryByLabelText(/şifre/i)).not.toBeInTheDocument();

    await userEvent.type(within(dialog).getByLabelText(/Giriş adı/), 'depo.yeni');
    await userEvent.type(within(dialog).getByLabelText(/Ad soyad/), 'Yeni Depocu');
    await userEvent.type(within(dialog).getByLabelText(/E-posta/), 'depo.yeni@iplik.local');
    await pickRole(dialog, 'Depo');
    await userEvent.click(await within(dialog).findByRole('switch', { name: 'Merkez Depo' }));
    await userEvent.click(within(dialog).getByRole('button', { name: /Hesabı aç/ }));

    expect(
      await within(dialog).findByText(/depo\.yeni@iplik\.local adresine gönderildi/),
    ).toBeInTheDocument();
    expect(within(dialog).queryByText(/Gecici-Sifre/)).not.toBeInTheDocument();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Tamam' }));

    const row = await screen.findByRole('row', { name: /depo\.yeni/ });
    expect(within(row).getByText('Depo')).toBeInTheDocument();
    expect(within(row).getByText('İlk giriş bekleniyor')).toBeInTheDocument();
  });

  it('eksik alanlar gönderilmeden altlarında ne yapılacağını söyler; istek gitmez', async () => {
    let isPosted = false;
    server.use(
      http.post(PATH, () => {
        isPosted = true;
        return new HttpResponse(null, { status: 201 });
      }),
    );
    renderApp('/panel/kullanicilar');
    await userEvent.click(await screen.findByRole('button', { name: 'Yeni kullanıcı' }));
    const dialog = await screen.findByRole('dialog');
    await userEvent.click(within(dialog).getByRole('button', { name: /Hesabı aç/ }));

    expect(await within(dialog).findByText('Bir rol seçin.')).toBeInTheDocument();
    expect(within(dialog).getByText('Ad ve soyadı girin.')).toBeInTheDocument();
    expect(within(dialog).getByText('En az bir lokasyon seçin.')).toBeInTheDocument();
    expect(isPosted).toBe(false);
  });

  it('giriş adı alınmışsa ya da e-posta gönderilemezse hata gösterir', async () => {
    server.use(
      http.post(
        PATH,
        () =>
          HttpResponse.json({ status: 502, code: 'LOGIN_ACCOUNT_UNAVAILABLE' }, { status: 502 }),
        { once: true },
      ),
    );
    renderApp('/panel/kullanicilar');
    await userEvent.click(await screen.findByRole('button', { name: 'Yeni kullanıcı' }));
    const dialog = await screen.findByRole('dialog');
    await userEvent.type(within(dialog).getByLabelText(/Giriş adı/), 'admin');
    await userEvent.type(within(dialog).getByLabelText(/Ad soyad/), 'Başka Biri');
    await userEvent.type(within(dialog).getByLabelText(/E-posta/), 'baska@iplik.local');
    await pickRole(dialog, 'Kalite');
    await userEvent.click(await within(dialog).findByRole('switch', { name: 'Merkez Depo' }));
    await userEvent.click(within(dialog).getByRole('button', { name: /Hesabı aç/ }));
    expect(await within(dialog).findByText(/e-posta sunucusuna ulaşılamadı/)).toBeInTheDocument();

    await userEvent.click(within(dialog).getByRole('button', { name: /Hesabı aç/ }));
    expect(
      await within(dialog).findByText('Bu giriş adı ya da e-posta başka bir hesapta kullanılıyor.'),
    ).toBeInTheDocument();
  });

  it('yönetici depo hesabının rolünü kaliteye çevirir; şoför hesabında rol değiştirme sunulmaz', async () => {
    renderApp('/panel/kullanicilar');
    const table = await screen.findByRole('table');
    const driver = within(table).getByRole('row', { name: /sofor/ });
    expect(within(driver).queryByRole('button', { name: /Rolü değiştir/ })).not.toBeInTheDocument();

    const depo = within(table).getByRole('row', { name: /Depo Sorumlusu/ });
    await userEvent.click(within(depo).getByRole('button', { name: 'depo rolünü değiştir' }));
    const dialog = await screen.findByRole('dialog');
    await pickRole(dialog, 'Kalite');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Rolü kaydet' }));

    const updated = await within(await screen.findByRole('table')).findByRole('row', {
      name: /Depo Sorumlusu.*Kalite/,
    });
    expect(within(updated).queryByText('Depo')).not.toBeInTheDocument();
  });

  it('rol değiştirilemezse sunucunun nedeni gösterilir ve pencere açık kalır', async () => {
    server.use(
      http.put(
        `${PATH}/:id/role`,
        () =>
          HttpResponse.json({ status: 409, code: 'LOGIN_ACCOUNT_ROLE_LOCKED' }, { status: 409 }),
        { once: true },
      ),
    );
    renderApp('/panel/kullanicilar');
    const table = await screen.findByRole('table');
    const depo = within(table).getByRole('row', { name: /Depo Sorumlusu/ });
    await userEvent.click(within(depo).getByRole('button', { name: 'depo rolünü değiştir' }));
    const dialog = await screen.findByRole('dialog');
    await pickRole(dialog, 'Müdür');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Rolü kaydet' }));

    expect(
      await within(dialog).findByText('Şoför ve fasoncu hesabının rolü değiştirilemez.'),
    ).toBeInTheDocument();
  });
});
