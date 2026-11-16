import { GENEL_MESAJ, test } from '../support/robot';
import type { Robot } from '../support/robot';
import { iplikTanimla } from '../support/akis';
import { damga, iplikSec, sec } from '../support/yardimci';

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
  robot.beklenen(/400 POST \/api\/v1\/central-stock/);
  // İplik serbest metin değil, Tanımlar'daki aktif iplikler arasından seçilir.
  const tanimli = damga();
  await iplikTanimla(mudur, tanimli);
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
      if (iplik !== '') await iplikSec(robot.sayfa, /İplik cinsi/, iplik);
      await robot.sayfa.getByLabel(/Gelen net kg/).fill(kg);
      await robot.sayfa.getByRole('button', { name: 'İplik girişini kaydet' }).click();
      await nedenliMesajVar(robot, `iplik girişi ${ad}`);
    });
  }
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

test('Formlar: müdür yeni sevkiyat formu eksik ve hatalı girişte uyarır', async ({ giris }) => {
  const robot = await giris('mudur');
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
      await robot.sayfa
        .getByLabel(/serbest metin/i)
        .first()
        .fill('ROBOT-iplik');
      await robot.sayfa.getByLabel(/Net kg 1/).fill(kg);
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
