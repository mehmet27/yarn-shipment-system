import type { Page } from '@playwright/test';
import { fiyatYaz, iplikTanimla, tedarikciTanimla } from '../support/akis';
import { rolTanimli, test } from '../support/robot';
import type { Robot } from '../support/robot';
import { damga, iplikGirisiDoldur } from '../support/yardimci';

/** Alanlara verilen kötü niyetli ya da uç değerler; sistem çökmemeli, 5xx vermemeli, betik çalıştırmamalı. */
const METINLER = [
  `ROBOT-${'A'.repeat(5000)}`,
  `ROBOT-${'😀🚀'.repeat(60)}`,
  'ROBOT-<img src=x onerror="window.__xss=1">',
  "ROBOT-'; DROP TABLE shipment; --",
  '   ',
  'ROBOT-‮evil',
  'ROBOT-../../etc/passwd',
  'ROBOT-%s%n%x{{7*7}}${7*7}',
] as const;
const SAYILAR = [
  '9'.repeat(40),
  '-0',
  '1e999',
  'NaN',
  '１２３',
  '0,0000001',
  '1,5,5',
  '--5',
] as const;

type Alan = { etiket: RegExp; tur: 'metin' | 'sayi'; ilk?: boolean };

async function sagliklimi(robot: Robot, ad: string) {
  const sayfa = robot.sayfa;
  await sayfa.waitForTimeout(700);
  const metin = await sayfa.innerText('body');
  robot.kontrol(
    `${ad}: çökme ekranı çıkmamalı`,
    !/Beklenmeyen bir hata|Aradığınız sayfa bulunamadı/.test(metin),
  );
  const xss = await sayfa.evaluate(() => (window as unknown as { __xss?: number }).__xss);
  robot.kontrol(`${ad}: girilen HTML çalıştırılmamalı (XSS)`, xss === undefined);
}

async function doldur(sayfa: Page, alan: Alan, deger: string) {
  const hedef =
    alan.ilk === true ? sayfa.getByLabel(alan.etiket).first() : sayfa.getByLabel(alan.etiket);
  await hedef.fill(deger).catch(() => undefined);
}

async function dene(
  robot: Robot,
  ad: string,
  alanlar: Alan[],
  gonder: () => Promise<void>,
  hazirla: () => Promise<void>,
) {
  for (const alan of alanlar) {
    const degerler = alan.tur === 'sayi' ? SAYILAR : METINLER;
    for (const deger of degerler) {
      await robot.adim(
        `${ad}: ${String(alan.etiket)} ← ${deger.slice(0, 24).replace(/\s/g, '·')}`,
        async () => {
          await hazirla();
          await doldur(robot.sayfa, alan, deger);
          await gonder();
          await sagliklimi(robot, `${ad} ${String(alan.etiket)}`);
        },
      );
    }
  }
}

test('Rastgele girdi: depo iplik girişi alanları', async ({ giris }) => {
  test.skip(!rolTanimli('depo') || !rolTanimli('mudur'), 'depo ya da mudur yok');
  const depo = await giris('depo');
  const mudur = await giris('mudur');
  depo.beklenen(/4\d\d (POST|PUT) /);
  // İplik serbest metin değil, Tanımlar'dan seçilir; bu yüzden metin fuzz'u yalnızca kilo, parti no, irsaliye no ve not alanlarına yapılır.
  const iplik = damga();
  const tedarikci = damga();
  await iplikTanimla(mudur, iplik);
  await tedarikciTanimla(mudur, tedarikci);
  await dene(
    depo,
    'iplik girişi',
    [
      { etiket: /Gelen net kg/, tur: 'sayi' },
      { etiket: /^Parti no/, tur: 'metin' },
      { etiket: /Tedarikçi irsaliye no/, tur: 'metin' },
      { etiket: /Not/, tur: 'metin' },
    ],
    async () => {
      await depo.sayfa.getByRole('button', { name: 'İplik girişini kaydet' }).click();
    },
    async () => {
      await depo.sayfa.goto('/panel/merkez-stok');
      await depo.sayfa.getByRole('tab', { name: 'İplik girişi' }).click();
      await iplikGirisiDoldur(depo.sayfa, { iplik, tedarikci, kg: '1' });
    },
  );
});

test('Rastgele girdi: müdür yeni sevkiyat alanları', async ({ giris }) => {
  test.skip(!rolTanimli('mudur'), 'mudur yok');
  const mudur = await giris('mudur');
  mudur.beklenen(/4\d\d (POST|PUT) /);
  await dene(
    mudur,
    'yeni sevkiyat',
    [
      { etiket: /Fire oranı/, tur: 'sayi' },
      { etiket: /Net kg 1/, tur: 'sayi' },
      { etiket: /ya da listede yoksa yazın/i, tur: 'metin', ilk: true },
      { etiket: /Sipariş no/, tur: 'metin' },
      { etiket: /İlgili kişi/, tur: 'metin' },
    ],
    async () => {
      await mudur.sayfa.getByRole('button', { name: 'Taslak oluştur' }).click();
    },
    async () => {
      await mudur.sayfa.goto('/panel/sevkiyat/yeni');
      await mudur.sayfa
        .getByLabel(/Alıcı fasoncu/)
        .first()
        .click();
      await mudur.sayfa.locator('[role=option]:visible').first().click();
      await mudur.sayfa.getByLabel(/Fire oranı/).fill('3');
      await mudur.sayfa
        .getByLabel(/ya da listede yoksa yazın/i)
        .first()
        .fill('ROBOT-fuzz');
      await mudur.sayfa.getByLabel(/Net kg 1/).fill('1');
      await fiyatYaz(mudur.sayfa);
    },
  );
});

test('Rastgele girdi: müdür tanım alanları', async ({ giris }) => {
  test.skip(!rolTanimli('mudur'), 'mudur yok');
  const mudur = await giris('mudur');
  mudur.beklenen(/4\d\d (POST|PUT) /);
  await dene(
    mudur,
    'fasoncu tanımı',
    [{ etiket: /Fasoncu adı/, tur: 'metin' }],
    async () => {
      await mudur.sayfa.getByRole('dialog').getByRole('button', { name: 'Kaydet' }).click();
    },
    async () => {
      await mudur.sayfa.goto('/panel/tanimlar');
      await mudur.sayfa.getByRole('button', { name: 'Ekle' }).click();
    },
  );
});

test('Rastgele girdi: yönetici kullanıcı alanları', async ({ giris }) => {
  test.skip(!rolTanimli('admin'), 'admin yok');
  const admin = await giris('admin');
  admin.beklenen(/4\d\d (POST|PUT) /);
  await dene(
    admin,
    'yeni kullanıcı',
    [
      { etiket: /Giriş adı/, tur: 'metin' },
      { etiket: /Ad soyad/, tur: 'metin' },
      { etiket: /İlk şifre/, tur: 'metin' },
    ],
    async () => {
      await admin.sayfa.getByRole('dialog').getByRole('button', { name: 'Hesabı aç' }).click();
    },
    async () => {
      await admin.sayfa.goto('/panel/kullanicilar');
      await admin.sayfa.getByRole('button', { name: 'Yeni kullanıcı' }).click();
    },
  );
});
