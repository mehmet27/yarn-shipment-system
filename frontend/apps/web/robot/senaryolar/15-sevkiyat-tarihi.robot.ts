import { expect, test } from '../support/robot';
import { cuvalEkle, taslakAc } from '../support/akis';
import { damga, istanbulGunu } from '../support/yardimci';

/** Müdür sevkiyat tarihini girer; depo o tarih gelmeden çuval atayamaz ve çıkış tartısı giremez. */
test('Sevkiyat tarihi: müdür tarih girer, depo tarih gelmeden işlem yapamaz', async ({ giris }) => {
  const depo = await giris('depo');
  const mudur = await giris('mudur');
  for (const r of [depo, mudur]) r.beklenen(/4\d\d (POST|PUT|PATCH) /);

  await mudur.adim(
    'form: tarih bugünle gelir; boş ya da geçmiş tarih nedenini söyler',
    async () => {
      await mudur.sayfa.goto('/panel/sevkiyat/yeni');
      const alan = mudur.sayfa.getByLabel(/Sevkiyat tarihi/);
      mudur.kontrol('tarih alanı bugünle dolu gelir', (await alan.inputValue()) === istanbulGunu());
      await alan.fill('');
      await mudur.sayfa.getByRole('button', { name: 'Taslak oluştur' }).click();
      await expect(mudur.sayfa.getByText('Sevkiyat tarihini seçin.')).toBeVisible();
      await alan.fill(istanbulGunu(-1));
      await mudur.sayfa.getByRole('button', { name: 'Taslak oluştur' }).click();
      await expect(mudur.sayfa.getByText('Sevkiyat tarihi bugünden önce olamaz.')).toBeVisible();
      mudur.kontrol('geçmiş tarihle sevkiyat oluşmamalı', mudur.sayfa.url().endsWith('/yeni'));
    },
  );

  const yarin = istanbulGunu(1);
  const [yil, ay, gun] = yarin.split('-');
  const id = await taslakAc(mudur, damga(), '100', '3', { tur: 'merkez' }, 'Başka Dokuma', yarin);

  await mudur.adim('liste ve dosya: sevkiyat tarihi görünür', async () => {
    await mudur.sayfa.goto('/panel/sevkiyat');
    await expect(mudur.sayfa.locator('main')).toContainText(
      `${gun ?? ''}.${ay ?? ''}.${yil ?? ''}`,
    );
    await mudur.sayfa.goto(`/panel/sevkiyat/${id}`);
    await expect(mudur.sayfa.locator('main')).toContainText(
      `${gun ?? ''}.${ay ?? ''}.${yil ?? ''}`,
    );
  });

  await depo.adim('tarih gelmeden: çıkış tartısı düğmesi yok, tarih uyarısı var', async () => {
    await depo.sayfa.goto(`/panel/sevkiyat/${id}`);
    await expect(depo.sayfa.getByText(/Bu tarihten önce çuval atanamaz/)).toBeVisible();
    depo.kontrol(
      'çıkış tartısı düğmesi görünmemeli',
      (await depo.sayfa.getByRole('link', { name: 'Çıkış tartısını gir' }).count()) === 0,
    );
    await expect(
      depo.sayfa.getByText(/Çıkış fotoğrafı, sevkiyat tarihi gelmeden eklenemez/),
    ).toBeVisible();
    depo.kontrol(
      'tarih gelmeden: fotoğraf çek ve dosya seç düğmeleri pasif olmalı',
      (await depo.sayfa.getByRole('button', { name: 'Fotoğraf çek' }).isDisabled()) &&
        (await depo.sayfa.getByRole('button', { name: 'Dosya seç' }).isDisabled()),
    );
    await depo.sayfa.goto(`/panel/sevkiyat/${id}/cikis`);
    await expect(depo.sayfa.getByText(/Bu tarihten önce çuval atanamaz/)).toBeVisible();
    depo.kontrol(
      'çıkış tartısı formu açılmamalı',
      (await depo.sayfa.getByLabel(/Dolu araç tartısı/).count()) === 0,
    );
  });

  await depo.adim(
    'tarih gelmeden: sunucu çuval atamayı, çıkış tartısını ve çıkış fotoğrafını da reddeder',
    async () => {
      const istekler = [
        { ad: 'bags', adres: `/api/v1/shipments/${id}/bags`, govde: { weightsKg: ['25.000'] } },
        {
          ad: 'dispatch-weighing',
          adres: `/api/v1/shipments/${id}/dispatch-weighing`,
          govde: { grossKg: '2000.000', tareKg: '1000.000', bagCount: 1 },
        },
        {
          ad: 'çıkış fotoğrafı',
          adres: '/api/v1/evidence/uploads',
          govde: {
            shipmentId: id,
            evidenceType: 'DISPATCH_SCALE',
            mimeType: 'image/jpeg',
            sizeBytes: 1000,
          },
        },
      ];
      for (const istek of istekler) {
        const kod = await depo.sayfa.evaluate(async ({ adres, govde }) => {
          const cerez = document.cookie.split('; ').find((c) => c.startsWith('XSRF-TOKEN='));
          const yanit = await fetch(adres, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'Idempotency-Key': crypto.randomUUID(),
              'X-XSRF-TOKEN': decodeURIComponent(cerez?.split('=')[1] ?? ''),
            },
            body: JSON.stringify(govde),
          });
          return `${String(yanit.status)} ${((await yanit.json()) as { code?: string }).code ?? ''}`;
        }, istek);
        depo.kontrol(
          `${istek.ad}: sunucu 409 SHIPMENT_NOT_DUE dönmeli`,
          kod === '409 SHIPMENT_NOT_DUE',
          kod,
        );
      }
    },
  );

  await mudur.adim(
    'tarih güncelleme: geçmiş güne alınamaz; bugüne alınınca depo işlem yapabilir',
    async () => {
      await mudur.sayfa.goto(`/panel/sevkiyat/${id}`);
      await mudur.sayfa.getByRole('button', { name: 'Tarihi değiştir' }).click();
      const alan = mudur.sayfa.getByLabel(/Yeni sevkiyat tarihi/);
      await alan.fill(istanbulGunu(-1));
      mudur.kontrol(
        'tarih alanının adı kısa: "(şu an ...)" eki yok',
        (await mudur.sayfa.getByText(/\(şu an/).count()) === 0,
      );
      mudur.kontrol(
        'geçmiş tarihle güncelle düğmesi pasif',
        await mudur.sayfa.getByRole('button', { name: 'Tarihi güncelle' }).isDisabled(),
      );
      await alan.fill(istanbulGunu());
      await mudur.sayfa.getByRole('button', { name: 'Tarihi güncelle' }).click();
      await expect(mudur.sayfa.getByRole('button', { name: 'Tarihi değiştir' })).toBeVisible();
      await expect(mudur.sayfa.getByText(/Bu tarihten önce çuval atanamaz/)).toHaveCount(0);
    },
  );

  await depo.adim(
    'tarih bugüne alındı: tarih uyarısı kalkar, çuvalsız tartı düğmesi pasif kalır',
    async () => {
      await depo.sayfa.goto(`/panel/sevkiyat/${id}`);
      await expect(depo.sayfa.getByText(/Bu tarihten önce çuval atanamaz/)).toHaveCount(0);
      await expect(depo.sayfa.getByRole('button', { name: 'Çıkış tartısını gir' })).toBeDisabled();
    },
  );

  await cuvalEkle(depo, id, 2, '50');

  await depo.adim(
    'tarih bugüne alındı: çuvallar eklenince depo çıkış tartısı bağlantısını görür',
    async () => {
      await depo.sayfa.goto(`/panel/sevkiyat/${id}`);
      await expect(depo.sayfa.getByRole('link', { name: 'Çıkış tartısını gir' })).toBeVisible();
      await expect(depo.sayfa.getByText(/Bu tarihten önce çuval atanamaz/)).toHaveCount(0);
    },
  );
});
