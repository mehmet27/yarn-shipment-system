// Koşu öncesi hazırlık (Playwright globalSetup): senaryolar başlamadan önce bir kez çalışır.
//  1. Yığın ayakta mı? Değilse her senaryonun ayrı ayrı zaman aşımına düşmesi yerine tek ve açık bir mesajla durur.
//  2. `admin` robotun bildiği şifreyle girebiliyor mu? Sıfırlamadan (`pnpm yerel:sifirla`) hemen sonra admin'in şifresi
//     `deploy/.env` içindeki geçici şifredir ve ilk girişte değiştirilmesi istenir. Hazırlık bu ilk girişi kendisi yapıp
//     robotun şifresini belirler; böylece sıfırlamadan sonraki ilk koşu elle bir şey yapmadan çalışır.
import { existsSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { chromium } from '@playwright/test';
import type { Browser, FullConfig } from '@playwright/test';
import { girisDeneSonuc, sifreDegistirFormu } from './yardimci';

const ROBOT = join(import.meta.dirname, '..');
const DEPO_KOKU = join(ROBOT, '..', '..', '..', '..');

function jsonOku(yol: string): Record<string, string> {
  return existsSync(yol) ? (JSON.parse(readFileSync(yol, 'utf8')) as Record<string, string>) : {};
}

/** `deploy/.env` içindeki bir değeri okur (dosya ya da anahtar yoksa `undefined`). */
function yerelOrtamDegeri(anahtar: string): string | undefined {
  const yol = join(DEPO_KOKU, 'deploy', '.env');
  if (!existsSync(yol)) return undefined;
  for (const satir of readFileSync(yol, 'utf8').split('\n')) {
    const eslesme = /^\s*([A-Z0-9_]+)\s*=\s*(.*?)\s*$/.exec(satir);
    if (eslesme?.[1] === anahtar && eslesme[2] !== undefined && eslesme[2] !== '')
      return eslesme[2];
  }
  return undefined;
}

function dur(mesaj: string): never {
  throw new Error(`\n\nROBOT HAZIRLIĞI DURDU: ${mesaj}\n`);
}

async function yiginAyaktaMi(adres: string) {
  let durum: number | string;
  try {
    durum = (await fetch(`${adres}/api/v1/me`, { signal: AbortSignal.timeout(10_000) })).status;
  } catch (hata) {
    durum = (hata as Error).message;
  }
  // Oturumsuz istek 401 döner; 200 de olabilir (ör. vekil oturum taşıyorsa). Başka her şey yığının hazır olmadığını gösterir.
  if (durum === 401 || durum === 200) return;
  dur(
    `Uygulamaya ulaşılamıyor (${adres}/api/v1/me → ${String(durum)}). Önce yığını başlatın: ` +
      '`cd frontend && pnpm yerel:baslat` (ya da temiz ortam için `pnpm yerel:sifirla`).',
  );
}

/** Tek giriş denemesi yapar; girdiyse sonunda bulunduğu yolu, giremediyse `undefined` döner. */
async function girisDene(tarayici: Browser, adres: string, kullanici: string, sifre: string) {
  const baglam = await tarayici.newContext({ baseURL: adres, locale: 'tr-TR' });
  const sayfa = await baglam.newPage();
  const girdi = await girisDeneSonuc(sayfa, kullanici, sifre).catch(() => false);
  return { baglam, sayfa, yol: girdi ? new URL(sayfa.url()).pathname : undefined };
}

export default async function hazirlik(ayar: FullConfig) {
  const kullan = ayar.projects[0]?.use;
  const adres = kullan?.baseURL ?? 'http://localhost:5173';
  await yiginAyaktaMi(adres);

  const admin = jsonOku(join(ROBOT, 'kullanicilar.json')).admin;
  const hedef = jsonOku(join(ROBOT, 'kullanicilar.local.json')).admin ?? process.env.ROBOT_SIFRE;
  // admin tanımlı değilse yapılacak hazırlık yoktur; admin isteyen senaryolar kendilerini atlar.
  if (admin === undefined || hedef === undefined || hedef === '') return;

  const tarayici = await chromium.launch(
    kullan?.channel === undefined ? {} : { channel: kullan.channel },
  );
  try {
    const ilk = await girisDene(tarayici, adres, admin, hedef);
    await ilk.baglam.close();
    if (ilk.yol !== undefined && !ilk.yol.includes('sifre-degistir')) return;
    if (ilk.yol !== undefined) {
      dur(
        `"${admin}" robotun şifresiyle girdi ama uygulama şifre değişikliği istiyor: robotun admin şifresi geçici şifreyle ` +
          'aynı. `ROBOT_SIFRE` (ya da `kullanicilar.local.json` içindeki admin şifresi) `deploy/.env` içindeki ' +
          '`LOCAL_TEST_USER_PASSWORD` değerinden farklı olmalı.',
      );
    }

    // Robotun şifresi tutmadı: sıfırlamadan sonraki geçici şifreyle ilk girişi yap ve robotun şifresini belirle.
    const gecici =
      process.env.ROBOT_ADMIN_ILK_SIFRE ?? yerelOrtamDegeri('LOCAL_TEST_USER_PASSWORD');
    if (gecici === undefined || gecici === hedef) {
      dur(
        `"${admin}" robotun bildiği şifreyle giremedi ve denenecek geçici şifre bulunamadı. ` +
          '`deploy/.env` içinde `LOCAL_TEST_USER_PASSWORD` tanımlı mı? Başka bir geçici şifre için `ROBOT_ADMIN_ILK_SIFRE` verin.',
      );
    }
    const ikinci = await girisDene(tarayici, adres, admin, gecici);
    try {
      if (ikinci.yol === undefined) {
        dur(
          `"${admin}" ne robotun şifresiyle ne de geçici şifreyle (\`deploy/.env\` → \`LOCAL_TEST_USER_PASSWORD\`) girebildi. ` +
            'Şifre elle değiştirildiyse `robot/kullanicilar.local.json` içine `{ "admin": "<şifre>" }` yazın ya da ' +
            '`cd frontend && pnpm yerel:sifirla` ile ortamı sıfırlayın. (Art arda hatalı denemeler hesabı kısa süre kilitler.)',
        );
      }
      // Örnek kullanıcılı ortamda (LOCAL_SAMPLE_USERS=true) şifre değişikliği istenmez; sayfayı kendimiz açarız.
      if (!ikinci.yol.includes('sifre-degistir')) await ikinci.sayfa.goto('/sifre-degistir');
      await sifreDegistirFormu(ikinci.sayfa, gecici, hedef);
      await ikinci.sayfa
        .waitForURL((u) => !/giris|sifre-degistir/.test(u.pathname), { timeout: 20_000 })
        .catch(() =>
          dur(
            `"${admin}" için robotun şifresi belirlenemedi (şifre kuralı: en az 8 karakter, yaygın şifre olmamalı, ` +
              'geçici şifreden farklı olmalı). `ROBOT_SIFRE` değerini denetleyin.',
          ),
        );
      console.log(
        `Robot hazırlığı: "${admin}" ilk girişi yapıldı, robotun şifresi belirlendi (sıfırlama sonrası).`,
      );
    } finally {
      await ikinci.baglam.close();
    }
  } finally {
    await tarayici.close();
  }
}
