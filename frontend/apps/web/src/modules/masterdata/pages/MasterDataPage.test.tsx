import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
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
    server.use(
      http.get('/api/v1/balances', () => HttpResponse.json({ items: [], totalKg: '0.000' })),
    );
    renderApp('/panel/tanimlar');
    const card = await screen.findByRole('group', { name: 'Çorlu Boya' });
    await userEvent.click(within(card).getByRole('button', { name: 'Pasife al' }));
    await userEvent.click(await screen.findByRole('switch', { name: 'Pasifleri göster' }));
    expect(await screen.findByText('Eski Dokuma')).toBeInTheDocument();
    expect(screen.getByText('Çorlu Boya')).toBeInTheDocument();
    expect((await screen.findAllByText('Pasif')).length).toBeGreaterThan(1);
  });

  it('bakiyesi olan fasoncu pasife alınırken uyarır, onaylanırsa pasife alır', async () => {
    server.use(...createHandlers('ADMIN'));
    server.use(
      http.get('/api/v1/balances', () =>
        HttpResponse.json({
          items: [
            {
              subcontractorId: 'sub-2',
              subcontractorTitle: 'Çorlu Boya',
              yarnType: 'Polyester 30/1',
              balanceKg: '1200.500',
              inTransitKg: '0.000',
            },
          ],
          totalKg: '1200.500',
        }),
      ),
    );
    renderApp('/panel/tanimlar');
    const card = await screen.findByRole('group', { name: 'Çorlu Boya' });
    await userEvent.click(within(card).getByRole('button', { name: 'Pasife al' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('açık bakiyesi var');
    expect(screen.getByText(/1\.200,5 kg/)).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Yine de pasife al' }));
    await userEvent.click(await screen.findByRole('switch', { name: 'Pasifleri göster' }));
    expect((await screen.findAllByText('Pasif')).length).toBeGreaterThan(1);
  });

  it('bakiyesi olan fasoncuda vazgeçilirse pasife alınmaz', async () => {
    server.use(...createHandlers('ADMIN'));
    server.use(
      http.get('/api/v1/balances', () =>
        HttpResponse.json({
          items: [
            {
              subcontractorId: 'sub-2',
              subcontractorTitle: 'Çorlu Boya',
              yarnType: 'Polyester 30/1',
              balanceKg: '10.000',
              inTransitKg: '0.000',
            },
          ],
          totalKg: '10.000',
        }),
      ),
    );
    renderApp('/panel/tanimlar');
    const card = await screen.findByRole('group', { name: 'Çorlu Boya' });
    await userEvent.click(within(card).getByRole('button', { name: 'Pasife al' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Vazgeç' }));
    expect(within(card).getByRole('button', { name: 'Pasife al' })).toBeInTheDocument();
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

  it('iplik kodla eklenir; aynı kod sunucu hatasıyla kod alanında gösterilir', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp('/panel/tanimlar');
    await userEvent.click(await screen.findByRole('tab', { name: 'İplikler' }));
    expect(await screen.findByText('PES-30-1')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Ekle' }));
    await userEvent.type(screen.getByLabelText(/^Kod/), 'pes-30-1');
    await userEvent.type(screen.getByLabelText(/İplik adı/), 'Polyester');
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }));
    expect(await screen.findByText('Bu kod başka bir kayıtta kullanılıyor.')).toBeInTheDocument();

    await userEvent.clear(screen.getByLabelText(/^Kod/));
    await userEvent.type(screen.getByLabelText(/^Kod/), 'pes-40-1');
    await userEvent.type(screen.getByLabelText(/İplik numarası/), 'Ne 40/1');
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }));
    expect(await screen.findByText('PES-40-1')).toBeInTheDocument();
  });

  it('kalite sınıfında geçersiz sırayı göndermeden reddeder; kod düzenlenemez', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp('/panel/tanimlar');
    await userEvent.click(await screen.findByRole('tab', { name: 'Kalite sınıfları' }));
    const card = await screen.findByRole('group', { name: 'İkinci kalite' });
    await userEvent.click(within(card).getByRole('button', { name: 'Düzenle' }));
    expect(screen.getByLabelText(/^Kod/)).toBeDisabled();
    await userEvent.clear(screen.getByLabelText(/Sıra/));
    await userEvent.type(screen.getByLabelText(/Sıra/), 'sarı');
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }));
    expect(
      await screen.findByText('Sırayı 0 ile 9999 arasında bir sayı olarak girin.'),
    ).toBeInTheDocument();
  });

  it('iplik tedarikçileri sekmesinde tedarikçi listelenir ve pasife alınabilir', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp('/panel/tanimlar');
    await userEvent.click(await screen.findByRole('tab', { name: 'İplik tedarikçileri' }));
    const card = await screen.findByRole('group', { name: 'Şireci' });
    await userEvent.click(within(card).getByRole('button', { name: 'Pasife al' }));
    expect(screen.queryByRole('group', { name: 'Şireci' })).not.toBeInTheDocument();
  });

  it('MASTER_DATA_MANAGE yetkisi olmayan rol bu sayfaya giremez', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/tanimlar');
    expect(await screen.findByText(/yetki/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Ekle' })).not.toBeInTheDocument();
  });
});
