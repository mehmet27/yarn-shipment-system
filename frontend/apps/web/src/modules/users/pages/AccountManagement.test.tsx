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
  it('yönetici depo hesabı açar; şifre sorulmaz ve gösterilmez, SMS gider, hesap ilk giriş bekleyen olarak görünür', async () => {
    renderApp('/panel/kullanicilar');
    await userEvent.click(await screen.findByRole('button', { name: 'Yeni kullanıcı' }));
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).queryByLabelText(/şifre/i)).not.toBeInTheDocument();

    await userEvent.type(within(dialog).getByLabelText(/Giriş adı/), 'depo.yeni');
    await userEvent.type(within(dialog).getByLabelText(/Ad soyad/), 'Yeni Depocu');
    await userEvent.type(within(dialog).getByLabelText(/Telefon/), '0532 123 45 67');
    await pickRole(dialog, 'Depo');
    await userEvent.click(await within(dialog).findByRole('switch', { name: 'Merkez Depo' }));
    await userEvent.click(within(dialog).getByRole('button', { name: /Hesabı aç/ }));

    expect(
      await within(dialog).findByText(
        /\+?0?532 123 45 67 numarasına gönderildi|numarasına gönderildi/,
      ),
    ).toBeInTheDocument();
    expect(within(dialog).queryByText(/Gecici-Sifre/)).not.toBeInTheDocument();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Tamam' }));

    const row = await screen.findByRole('row', { name: /depo\.yeni/ });
    expect(within(row).getByText('Depo')).toBeInTheDocument();
    expect(within(row).getByText('***4567')).toBeInTheDocument();
    expect(within(row).getByText('İlk giriş bekleniyor')).toBeInTheDocument();
  });

  it('fasoncu hesabı kullanıcı ekranından, fasoncu kaydına bağlanarak açılır', async () => {
    renderApp('/panel/kullanicilar');
    await userEvent.click(await screen.findByRole('button', { name: 'Yeni kullanıcı' }));
    const dialog = await screen.findByRole('dialog');
    await pickRole(dialog, 'Fasoncu');
    await userEvent.type(within(dialog).getByLabelText(/Giriş adı/), 'yeni.fasoncu');
    await userEvent.type(within(dialog).getByLabelText(/Telefon/), '05329998877');
    await userEvent.click(await within(dialog).findByRole('combobox', { name: /Fasoncu kaydı/ }));
    await userEvent.click(await screen.findByRole('option', { name: 'Bursa Tekstil' }));
    await userEvent.click(await within(dialog).findByRole('switch', { name: 'Merkez Depo' }));
    await userEvent.click(within(dialog).getByRole('button', { name: /Hesabı aç/ }));
    expect(await within(dialog).findByText(/numarasına gönderildi/)).toBeInTheDocument();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Tamam' }));

    const row = await screen.findByRole('row', { name: /yeni\.fasoncu/ });
    expect(within(row).getByText('Fasoncu')).toBeInTheDocument();
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
    expect(within(dialog).getByText(/Geçerli bir cep telefonu girin/)).toBeInTheDocument();
    expect(within(dialog).getByText('En az bir lokasyon seçin.')).toBeInTheDocument();
    expect(isPosted).toBe(false);
  });

  it('SMS gönderilemezse hata gösterilir; ikinci denemede giriş adı alınmışsa o hata görünür', async () => {
    server.use(
      http.post(
        PATH,
        () => HttpResponse.json({ status: 503, code: 'SMS_UNAVAILABLE' }, { status: 503 }),
        { once: true },
      ),
    );
    renderApp('/panel/kullanicilar');
    await userEvent.click(await screen.findByRole('button', { name: 'Yeni kullanıcı' }));
    const dialog = await screen.findByRole('dialog');
    await userEvent.type(within(dialog).getByLabelText(/Giriş adı/), 'admin');
    await userEvent.type(within(dialog).getByLabelText(/Ad soyad/), 'Başka Biri');
    await userEvent.type(within(dialog).getByLabelText(/Telefon/), '05321234567');
    await pickRole(dialog, 'Kalite');
    await userEvent.click(await within(dialog).findByRole('switch', { name: 'Merkez Depo' }));
    await userEvent.click(within(dialog).getByRole('button', { name: /Hesabı aç/ }));
    expect(await within(dialog).findByText(/SMS gönderilemedi/)).toBeInTheDocument();

    await userEvent.click(within(dialog).getByRole('button', { name: /Hesabı aç/ }));
    expect(
      await within(dialog).findByText('Bu giriş adı ya da e-posta başka bir hesapta kullanılıyor.'),
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

  it('yönetici şifre sıfırlamayı başlatır; telefon maskeli görünür ve şifre gösterilmez', async () => {
    renderApp('/panel/kullanicilar');
    const table = await screen.findByRole('table');
    const partner = within(table).getByRole('row', { name: /Aydın Örme/ });
    await userEvent.click(
      within(partner).getByRole('button', { name: 'fasoncu için şifre sıfırlamayı başlat' }),
    );
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText(/\*\*\*0005/)).toBeInTheDocument();
    await userEvent.click(within(dialog).getByRole('button', { name: 'SMS gönder' }));
    expect(await within(dialog).findByText(/Kod .*0005 numarasına gönderildi/)).toBeInTheDocument();
  });

  it('yönetici depo hesabının rolünü kaliteye çevirir; fasoncu hesabında rol değiştirme sunulmaz', async () => {
    renderApp('/panel/kullanicilar');
    const table = await screen.findByRole('table');
    const partner = within(table).getByRole('row', { name: /Aydın Örme/ });
    expect(
      within(partner).queryByRole('button', { name: /rolünü değiştir/ }),
    ).not.toBeInTheDocument();

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
