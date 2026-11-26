import { expect, GENEL_MESAJ, test } from '../support/robot';
import type { Robot } from '../support/robot';
import {
  fiyatYaz,
  iplikTanimla,
  kaliteSinifiTanimla,
  kumasFormuDoldur,
  kumasGir,
  oranYaz,
  sevkiyatIpligiSec,
  tamamlanmisSevkiyat,
  tedarikciTanimla,
} from '../support/akis';
import { damga, iplikGirisiDoldur, iplikSec, sec, tedarikciSec } from '../support/yardimci';

/** Geçersiz girişte ekran, nedeni söyleyen bir mesaj göstermeli (genel "tekrar deneyin" yetmez). */
async function nedenliMesajVar(robot: Robot, ad: string) {
  const mesajlar = await robot.mesajlar();
  robot.kontrol(`${ad}: ekranda bir mesaj görünmeli`, mesajlar.length > 0);
  robot.kontrol(
    `${ad}: mesaj nedeni söylemeli`,
    !mesajlar.some((m) => GENEL_MESAJ.test(m)),
    mesajlar.join(' | '),
  );
}

test('Formlar: depo iplik girişi geçersiz değerleri reddeder', async ({ giris }) => {
  const robot = await giris('depo');
  const mudur = await giris('mudur');
  robot.beklenen(/4\d\d POST \/api\/v1\/central-stock/);
  // İplik ve tedarikçi serbest metin değil, Tanımlar'daki aktif kayıtlar arasından seçilir.
  const tanimli = damga();
  const tedarikci = damga();
  const digerTedarikci = damga();
  const sinif = damga();
  await iplikTanimla(mudur, tanimli);
  await tedarikciTanimla(mudur, tedarikci);
  await tedarikciTanimla(mudur, digerTedarikci);
  await kaliteSinifiTanimla(mudur, sinif);
  await robot.sayfa.goto('/panel/merkez-stok');
  await robot.sayfa.getByRole('tab', { name: 'İplik girişi' }).click();
  await robot.adim("iplik girişi: Tanımlar'da olmayan iplik seçilemez", async () => {
    const alan = robot.sayfa.getByLabel(/İplik cinsi/).first();
    await alan.click();
    await alan.fill(`${tanimli}-tanimsiz`);
    robot.kontrol(
      'listede olmayan iplik için seçenek çıkmamalı',
      (await robot.sayfa.locator('[role=option]:visible').count()) === 0,
    );
    await robot.sayfa.getByLabel(/Gelen net kg/).fill('5');
    await robot.sayfa.getByRole('button', { name: 'İplik girişini kaydet' }).click();
    await nedenliMesajVar(robot, 'iplik girişi tanımsız iplik');
  });
  for (const [ad, iplik, kg] of [
    ['boş', '', ''],
    ['kilo 0', tanimli, '0'],
    ['kilo eksi', tanimli, '-5'],
    ['kilo harf', tanimli, 'abc'],
    ['kilo 4 ondalık', tanimli, '1,2345'],
    ['kilo 100.000 üstü', tanimli, '100001'],
  ] as const) {
    await robot.adim(`iplik girişi: ${ad}`, async () => {
      if (iplik !== '') {
        await iplikSec(robot.sayfa, /İplik cinsi/, iplik);
        await tedarikciSec(robot.sayfa, tedarikci);
      }
      await robot.sayfa.getByLabel(/Gelen net kg/).fill(kg);
      await robot.sayfa.getByRole('button', { name: 'İplik girişini kaydet' }).click();
      await nedenliMesajVar(robot, `iplik girişi ${ad}`);
    });
  }
  await robot.adim('iplik girişi: tedarikçi seçilmeden kaydedilmez', async () => {
    await robot.sayfa.reload();
    await robot.sayfa.getByRole('tab', { name: 'İplik girişi' }).click();
    await iplikSec(robot.sayfa, /İplik cinsi/, tanimli);
    await robot.sayfa.getByLabel(/Gelen net kg/).fill('5');
    await robot.sayfa.getByRole('button', { name: 'İplik girişini kaydet' }).click();
    await nedenliMesajVar(robot, 'iplik girişi tedarikçisiz');
  });
  for (const [ad, parti, irsaliye] of [
    ['parti no Türkçe harf', 'ğüş', ''],
    ['parti no çok uzun', 'P'.repeat(41), ''],
    ['irsaliye no çok uzun', '', 'I'.repeat(129)],
  ] as const) {
    await robot.adim(`iplik girişi: ${ad}`, async () => {
      await robot.sayfa.reload();
      await robot.sayfa.getByRole('tab', { name: 'İplik girişi' }).click();
      await iplikGirisiDoldur(robot.sayfa, { iplik: tanimli, tedarikci, kg: '5' });
      if (parti !== '') await robot.sayfa.getByRole('combobox', { name: /^Parti no/ }).fill(parti);
      if (irsaliye !== '') await robot.sayfa.getByLabel(/Tedarikçi irsaliye no/).fill(irsaliye);
      await robot.sayfa.getByRole('button', { name: 'İplik girişini kaydet' }).click();
      await nedenliMesajVar(robot, `iplik girişi ${ad}`);
    });
  }
  const parti = `rb-${damga().replaceAll(/\D/g, '')}`;
  await robot.adim('iplik girişi: yeni parti için kalite sınıfı istenir', async () => {
    await robot.sayfa.reload();
    await robot.sayfa.getByRole('tab', { name: 'İplik girişi' }).click();
    await iplikGirisiDoldur(robot.sayfa, { iplik: tanimli, tedarikci, kg: '5' });
    await robot.sayfa.getByRole('combobox', { name: /^Parti no/ }).fill(parti);
    await robot.sayfa.getByRole('button', { name: 'İplik girişini kaydet' }).click();
    await expect(robot.sayfa.getByText(/kalite sınıfını seçin/)).toBeVisible();
    await sec(robot.sayfa, /^Kalite sınıfı/, sinif);
    await robot.sayfa.getByRole('button', { name: 'İplik girişini kaydet' }).click();
    await robot.sayfa.getByText('İplik girişi kaydedildi.').waitFor();
  });
  await robot.adim(
    'iplik girişi: kayıtlı parti listeden seçilir, kalite sınıfı sorulmaz',
    async () => {
      await robot.sayfa.reload();
      await robot.sayfa.getByRole('tab', { name: 'İplik girişi' }).click();
      await iplikGirisiDoldur(robot.sayfa, { iplik: tanimli, tedarikci, kg: '5' });
      await robot.sayfa.getByRole('combobox', { name: /^Parti no/ }).click();
      await robot.sayfa.getByRole('option', { name: parti.toUpperCase(), exact: true }).click();
      await expect(robot.sayfa.getByText('Kayıtlı parti.')).toBeVisible();
      await expect(robot.sayfa.getByLabel(/^Kalite sınıfı/)).toHaveCount(0);
      await robot.sayfa.getByRole('button', { name: 'İplik girişini kaydet' }).click();
      await robot.sayfa.getByText('İplik girişi kaydedildi.').waitFor();
    },
  );
  await robot.adim('iplik girişi: parti başka tedarikçiye aitse kaydedilmez', async () => {
    await robot.sayfa.reload();
    await robot.sayfa.getByRole('tab', { name: 'İplik girişi' }).click();
    await iplikGirisiDoldur(robot.sayfa, { iplik: tanimli, tedarikci: digerTedarikci, kg: '5' });
    await robot.sayfa.getByRole('combobox', { name: /^Parti no/ }).fill(parti);
    await expect(
      robot.sayfa.getByText(/başka bir iplik cinsine ya da tedarikçiye ait/),
    ).toBeVisible();
    await robot.sayfa.getByRole('button', { name: 'İplik girişini kaydet' }).click();
    await expect(robot.sayfa.getByText('Bu parti no başka bir tedarikçiye ait.')).toBeVisible();
  });
});

test('Formlar: kalite kumaş girişi stokta olmayan iplikle üretimi reddeder', async ({ giris }) => {
  const robot = await giris('kalite');
  const mudur = await giris('mudur');
  robot.beklenen(/40\d POST \/api\/v1\/central-stock/);
  // Stokta hiç girişi olmayan ama Tanımlar'da aktif bir iplik: seçilebilir, üretim stok yetersizliğiyle reddedilir.
  const stoksuz = `${damga()}-yok`;
  await iplikTanimla(mudur, stoksuz);
  await robot.sayfa.goto('/panel/merkez-stok');
  await robot.sayfa.getByRole('tab', { name: 'Kumaş girişi' }).click();
  for (const [ad, k] of [
    ['boş', { kumas: '', iplik: '', kg: '', fire: '' }],
    ['fire 150', { kumas: 'Süprem', iplik: stoksuz, kg: '5', fire: '150' }],
    ['stokta olmayan iplik', { kumas: 'Süprem', iplik: stoksuz, kg: '5', fire: '3' }],
  ] as const) {
    await robot.adim(`kumaş girişi: ${ad}`, async () => {
      await robot.sayfa.getByLabel(/Kumaş cinsi/).fill(k.kumas);
      if (k.iplik !== '') await iplikSec(robot.sayfa, /İplik cinsi 1/, k.iplik);
      await robot.sayfa.getByLabel(/Kumaş kg 1/).fill(k.kg);
      await robot.sayfa.getByLabel(/Fire \(%\)/).fill(k.fire);
      await robot.sayfa.getByRole('button', { name: 'Kumaşı kaydet' }).click();
      await nedenliMesajVar(robot, `kumaş girişi ${ad}`);
    });
  }
});

test('Formlar: kumaş girişi oran ve iplik yetersizliğinde, kalite itirazı kilo sınırını aşınca uyarır', async ({
  giris,
}) => {
  const depo = await giris('depo');
  const mudur = await giris('mudur');
  const kalite = await giris('kalite');
  const fasonKalite = await giris('fason-kalite');
  for (const r of [depo, mudur, kalite, fasonKalite]) r.beklenen(/4\d\d (POST|GET) /);
  const id = await tamamlanmisSevkiyat({ depo, mudur }, damga(), '100', '3');

  await fasonKalite.adim('kumaş girişi: oran yazılmadan kontrol edilemez', async () => {
    await kumasFormuDoldur(fasonKalite, id, '50', `${damga()}-K`);
    await fasonKalite.sayfa.getByRole('button', { name: 'Kontrol et' }).click();
    await nedenliMesajVar(fasonKalite, 'kumaş girişi oransız');
    fasonKalite.kontrol(
      'oransız kontrolde onay sorusu çıkmamalı',
      (await fasonKalite.sayfa.getByRole('button', { name: 'Evet, kaydet' }).count()) === 0,
    );
  });

  await fasonKalite.adim('kumaş girişi: oranların toplamı 100 değilse uyarılır', async () => {
    await fasonKalite.sayfa
      .getByLabel(/^Oran: /)
      .first()
      .fill('60');
    await expect(fasonKalite.sayfa.getByTestId('fabric-ratio-total')).toHaveText(
      'Toplam: %60 (tam %100 olmalı)',
    );
    await fasonKalite.sayfa.getByRole('button', { name: 'Kontrol et' }).click();
    await expect(fasonKalite.sayfa.getByText('Oranların toplamı tam %100 olmalı.')).toBeVisible();
    fasonKalite.kontrol(
      'toplamı 100 olmayan oranda onay sorusu çıkmamalı',
      (await fasonKalite.sayfa.getByRole('button', { name: 'Evet, kaydet' }).count()) === 0,
    );
  });

  await fasonKalite.adim(
    'kumaş girişi: fasoncudaki ipliği aşan kilo için eksik kilo yazılır, kaydedilemez',
    async () => {
      await kumasFormuDoldur(fasonKalite, id, '101', `${damga()}-K`);
      await oranYaz(fasonKalite);
      await fasonKalite.sayfa.getByRole('button', { name: 'Kontrol et' }).click();
      await expect(fasonKalite.sayfa.getByText('Fasoncuda yeterli iplik yok')).toBeVisible();
      await expect(fasonKalite.sayfa.getByText(/1 kg eksik/)).toBeVisible();
      fasonKalite.kontrol(
        'iplik yetmeyince onay sorusu çıkmamalı',
        (await fasonKalite.sayfa.getByRole('button', { name: 'Evet, kaydet' }).count()) === 0,
      );
    },
  );

  await kalite.adim('kalite itirazı: kumaş girişi yokken form açılmaz, nedeni yazar', async () => {
    await kalite.sayfa.goto(`/panel/sevkiyat/${id}`);
    await kalite.sayfa.getByRole('tab', { name: 'Kalite itirazı', exact: true }).click();
    await expect(kalite.sayfa.getByText(/önce kumaş girişi yapılmalı/)).toBeVisible();
    await expect(kalite.sayfa.getByRole('button', { name: 'Kalite itirazı aç' })).toHaveCount(0);
  });

  await fasonKalite.adim('kumaş girişi: 60 kg kumaş kaydedilir', async () => {
    await kumasGir(fasonKalite, id, '60', `${damga()}-K2`);
  });

  await kalite.adim(
    'kalite itirazı: girilen kumaştan fazla kilo uyarılır, gönder kapalı',
    async () => {
      await kalite.sayfa.goto(`/panel/sevkiyat/${id}`);
      await kalite.sayfa.getByRole('tab', { name: 'Kalite itirazı', exact: true }).click();
      await kalite.sayfa.getByRole('button', { name: 'Kalite itirazı aç' }).click();
      await expect(
        kalite.sayfa.getByText('Girilen kumaşa göre en çok 60 kg yazabilirsiniz.'),
      ).toBeVisible();
      await kalite.sayfa.getByLabel(/İtiraz edilen kilo/).fill('61');
      await kalite.sayfa.getByLabel(/Sorunu anlatın/).fill('Robot: fazla kilo');
      await nedenliMesajVar(kalite, 'itiraz 61 kg');
      await expect(kalite.sayfa.getByRole('button', { name: 'İtirazı gönder' })).toBeDisabled();
    },
  );
});

test('Formlar: müdür yeni sevkiyat formu eksik ve hatalı girişte uyarır', async ({ giris }) => {
  const robot = await giris('mudur');
  const sevkiyatIpligi = damga();
  await iplikTanimla(robot, sevkiyatIpligi);
  await robot.sayfa.goto('/panel/sevkiyat/yeni');
  await robot.adim('hepsi boş', async () => {
    await robot.sayfa.getByRole('button', { name: 'Taslak oluştur' }).click();
    await nedenliMesajVar(robot, 'yeni sevkiyat boş');
  });
  await sec(robot.sayfa, /Alıcı fasoncu/, 'Başka Dokuma');
  for (const [ad, fire, kg] of [
    ['fire eksi', '-3', '10'],
    ['fire 150', '150', '10'],
    ['kilo 0', '3', '0'],
    ['kilo harf', '3', 'abc'],
    ['kilo 100.000 üstü', '3', '100001'],
  ] as const) {
    await robot.adim(`yeni sevkiyat: ${ad}`, async () => {
      await robot.sayfa.getByLabel(/Fire oranı/).fill(fire);
      await sevkiyatIpligiSec(robot.sayfa, sevkiyatIpligi);
      await robot.sayfa.getByLabel(/Net kg 1/).fill(kg);
      await fiyatYaz(robot.sayfa);
      await robot.sayfa.getByRole('button', { name: 'Taslak oluştur' }).click();
      await nedenliMesajVar(robot, `yeni sevkiyat ${ad}`);
      robot.kontrol(
        `yeni sevkiyat ${ad}: sevkiyat oluşmamalı`,
        robot.sayfa.url().endsWith('/yeni'),
      );
    });
  }
});

test('Formlar: yönetici kullanıcı oluştururken geçersiz alanları yakalar', async ({ giris }) => {
  const robot = await giris('admin');
  robot.beklenen(
    /409 POST \/api\/v1\/master-data\/login-accounts/,
    /400 POST \/api\/v1\/master-data\/login-accounts/,
  );
  await robot.sayfa.goto('/panel/kullanicilar');
  await robot.sayfa.getByRole('button', { name: 'Yeni kullanıcı' }).click();
  const pencere = robot.sayfa.getByRole('dialog');
  await robot.adim('hepsi boş', async () => {
    await pencere.getByRole('button', { name: 'Hesabı aç' }).click();
    await nedenliMesajVar(robot, 'kullanıcı boş');
  });
  await sec(robot.sayfa, /^Rol/, 'Depo', pencere);
  for (const [ad, giris_, adSoyad, sifre] of [
    ['büyük harfli ad', 'Yeni', 'Yeni Kişi', 'Robot!2026a'],
    ['türkçe karakter', 'müdür2', 'Yeni Kişi', 'Robot!2026a'],
    ['kısa şifre', 'robot.test', 'Yeni Kişi', '123'],
    ['mevcut giriş adı', 'mudur', 'Başka Müdür', 'Robot!2026a'],
  ] as const) {
    await robot.adim(`kullanıcı: ${ad}`, async () => {
      await pencere.getByLabel(/Giriş adı/).fill(giris_);
      await pencere.getByLabel(/Ad soyad/).fill(adSoyad);
      await pencere.getByLabel(/İlk şifre/).fill(sifre);
      await pencere.getByRole('button', { name: 'Hesabı aç' }).click();
      await nedenliMesajVar(robot, `kullanıcı ${ad}`);
    });
  }
});
