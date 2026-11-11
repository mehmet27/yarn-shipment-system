import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
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
  it('yönetici depo hesabı açar; ilk şifreyi yazar, hesap ilk giriş bekleyen olarak görünür', async () => {
    renderApp('/panel/kullanicilar');
    await userEvent.click(await screen.findByRole('button', { name: 'Yeni kullanıcı' }));
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).queryByLabelText(/Telefon/)).not.toBeInTheDocument();
    expect(within(dialog).queryByLabelText(/E-posta/)).not.toBeInTheDocument();
    expect(within(dialog).queryByText(/lokasyon/i)).not.toBeInTheDocument();

    await userEvent.type(within(dialog).getByLabelText(/Giriş adı/), 'depo.yeni');
    await userEvent.type(within(dialog).getByLabelText(/Ad soyad/), 'Yeni Depocu');
    await userEvent.type(within(dialog).getByLabelText(/İlk şifre/), 'Gecici-Sifre-1');
    await pickRole(dialog, 'Depo');
    await userEvent.click(within(dialog).getByRole('button', { name: /Hesabı aç/ }));

    expect(await within(dialog).findByText(/depo\.yeni hesabı açıldı/)).toBeInTheDocument();
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
    expect(within(dialog).getByText('Şifre en az 8 karakter olmalı.')).toBeInTheDocument();
    expect(isPosted).toBe(false);
  });

  it('şifre kurala uymazsa hata gösterilir; ikinci denemede giriş adı alınmışsa o hata görünür', async () => {
    server.use(
      http.post(
        PATH,
        () => HttpResponse.json({ status: 400, code: 'WEAK_PASSWORD' }, { status: 400 }),
        { once: true },
      ),
    );
    renderApp('/panel/kullanicilar');
    await userEvent.click(await screen.findByRole('button', { name: 'Yeni kullanıcı' }));
    const dialog = await screen.findByRole('dialog');
    await userEvent.type(within(dialog).getByLabelText(/Giriş adı/), 'admin');
    await userEvent.type(within(dialog).getByLabelText(/Ad soyad/), 'Başka Biri');
    await userEvent.type(within(dialog).getByLabelText(/İlk şifre/), '12345678');
    await pickRole(dialog, 'Kalite');
    await userEvent.click(within(dialog).getByRole('button', { name: /Hesabı aç/ }));
    expect(await within(dialog).findByText(/Şifre kurallara uymuyor/)).toBeInTheDocument();

    await userEvent.click(within(dialog).getByRole('button', { name: /Hesabı aç/ }));
    expect(
      await within(dialog).findByText('Bu giriş adı başka bir hesapta kullanılıyor.'),
    ).toBeInTheDocument();
  });

  it('yönetici hesabı pasife alır ve yeniden açar', async () => {
    renderApp('/panel/kullanicilar');
    const table = await screen.findByRole('table');
    const depo = within(table).getByRole('row', { name: /Depo Sorumlusu/ });
    await userEvent.click(within(depo).getByRole('button', { name: 'depo hesabını pasife al' }));
    const confirm = await screen.findByRole('dialog', { name: 'Hesabı pasife al' });
    await userEvent.click(within(confirm).getByRole('button', { name: 'Pasife al' }));
    const disabled = await within(await screen.findByRole('table')).findByRole('row', {
      name: /Depo Sorumlusu.*Devre dışı/,
    });
    await userEvent.click(
      within(disabled).getByRole('button', { name: 'depo hesabını yeniden aç' }),
    );
    expect(
      await within(await screen.findByRole('table')).findByRole('row', {
        name: /Depo Sorumlusu.*Etkin/,
      }),
    ).toBeInTheDocument();
  });

  it('pasife alma onay ister; vazgeçilirse hesap etkin kalır', async () => {
    renderApp('/panel/kullanicilar');
    const table = await screen.findByRole('table');
    const depo = within(table).getByRole('row', { name: /Depo Sorumlusu/ });
    await userEvent.click(within(depo).getByRole('button', { name: 'depo hesabını pasife al' }));
    const confirm = await screen.findByRole('dialog', { name: 'Hesabı pasife al' });
    await userEvent.click(within(confirm).getByRole('button', { name: 'Vazgeç' }));
    expect(
      within(await screen.findByRole('table')).getByRole('row', { name: /Depo Sorumlusu.*Etkin/ }),
    ).toBeInTheDocument();
  });

  it('rozet metinleri kesilmez: kırpma yerine alt satıra iner', () => {
    const css = readFileSync(
      resolve(__dirname, '../../../../../../packages/ui/src/styles.css'),
      'utf8',
    );
    expect(css).toMatch(/\.mantine-Badge-label\s*{[^}]*text-overflow:\s*clip/);
    expect(css).toMatch(/\.mantine-Badge-label\s*{[^}]*white-space:\s*normal/);
  });

  it('yönetici yeni geçici şifre verir; şifre gösterilmez, hesap ilk giriş bekleyen olur', async () => {
    renderApp('/panel/kullanicilar');
    const table = await screen.findByRole('table');
    const depo = within(table).getByRole('row', { name: /Depo Sorumlusu/ });
    await userEvent.click(within(depo).getByRole('button', { name: 'depo için yeni şifre ver' }));
    const dialog = await screen.findByRole('dialog');
    const submit = within(dialog).getByRole('button', { name: 'Şifreyi kaydet' });
    expect(submit).toBeDisabled();
    await userEvent.type(within(dialog).getByLabelText(/İlk şifre/), 'Yeni-Sifre-2026');
    await userEvent.click(submit);
    expect(
      await within(dialog).findByText(/depo için yeni geçici şifre verildi/),
    ).toBeInTheDocument();
    expect(within(dialog).queryByText(/Yeni-Sifre-2026/)).not.toBeInTheDocument();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Tamam' }));
    expect(
      await within(await screen.findByRole('table')).findByRole('row', {
        name: /Depo Sorumlusu.*İlk giriş bekleniyor/,
      }),
    ).toBeInTheDocument();
  });

  it('yönetici depo hesabının rolünü kaliteye çevirir', async () => {
    renderApp('/panel/kullanicilar');
    const table = await screen.findByRole('table');
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
