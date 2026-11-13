import { readFileSync, existsSync } from 'node:fs';
import { join } from 'node:path';
import { test as base, expect } from '@playwright/test';
import type { BrowserContext, Page, TestInfo } from '@playwright/test';

export type Sorun = { tur: 'konsol' | 'ag' | 'sayfa' | 'beklenti'; ozet: string; ekran?: string };
export type Adim = { ad: string; sonuc: 'ok' | 'sorun' };

const GATEWAY = process.env.ROBOT_GATEWAY ?? 'http://localhost:8080';
const KULLANICILAR = JSON.parse(
  readFileSync(join(import.meta.dirname, '..', 'kullanicilar.json'), 'utf8'),
) as Record<string, string>;
const YEREL = join(import.meta.dirname, '..', 'kullanicilar.local.json');
const OZEL_SIFRE: Record<string, string> = existsSync(YEREL)
  ? (JSON.parse(readFileSync(YEREL, 'utf8')) as Record<string, string>)
  : {};

/** Bu rol için hesap ve şifre tanımlı mı? Değilse senaryo atlanır. */
export function rolTanimli(rol: string): boolean {
  return (
    KULLANICILAR[rol] !== undefined && (OZEL_SIFRE[rol] ?? process.env.ROBOT_SIFRE) !== undefined
  );
}

/** Bilinçli olarak üretilen sunucu hataları (ör. doğrulama 400'ü) sorun sayılmaz. */
export type Beklenen = RegExp;

/** Tek bir rolün oturumu: sayfa, toplanan sorunlar ve adım kaydı. */
export class Robot {
  readonly sorunlar: Sorun[] = [];
  readonly adimlar: Adim[] = [];
  private beklenenler: Beklenen[] = [/401 GET \/api\/v1\/me/];

  constructor(
    readonly sayfa: Page,
    readonly baglam: BrowserContext,
    private readonly info: TestInfo,
    readonly rol: string,
  ) {
    sayfa.on('pageerror', (hata) =>
      this.sorunEkle('sayfa', `Yakalanmamış istisna: ${hata.message}`),
    );
    sayfa.on('console', (mesaj) => {
      // 4xx ağ satırları 'response' dinleyicisinden gelir; ERR_INSUFFICIENT_RESOURCES uzun koşuda tarayıcının kaynak sınırıdır.
      if (
        mesaj.type() !== 'error' ||
        /status of 4\d\d|ERR_INSUFFICIENT_RESOURCES/.test(mesaj.text())
      )
        return;
      this.sorunEkle('konsol', mesaj.text().slice(0, 300));
    });
    sayfa.on('response', (yanit) => {
      if (yanit.status() < 400) return;
      const yol = yanit.url().replace(/^https?:\/\/[^/]+/, '');
      const satir = `${String(yanit.status())} ${yanit.request().method()} ${yol}`.slice(0, 200);
      if (this.beklenenler.some((kalip) => kalip.test(satir))) return;
      this.sorunEkle('ag', satir);
    });
  }

  /** Bu işlemde dönmesi doğal olan hata yanıtlarını (ör. `/400 POST \/api\/v1\/central-stock/`) sorun saymaz. */
  beklenen(...kaliplar: Beklenen[]) {
    this.beklenenler.push(...kaliplar);
  }

  private sorunEkle(tur: Sorun['tur'], ozet: string) {
    if (!this.sorunlar.some((s) => s.ozet === ozet)) this.sorunlar.push({ tur, ozet });
  }

  /** Adımı çalıştırır; adımın içinde sorun çıkarsa ekran görüntüsü alınır ve adım "sorun" işaretlenir. */
  async adim<T>(ad: string, is: () => Promise<T>): Promise<T | undefined> {
    const once = this.sorunlar.length;
    if (process.env.ROBOT_DETAY === '1') console.log(`  → ${this.rol}: ${ad}`);
    try {
      const sonuc = await is();
      await this.kaydet(ad, once);
      return sonuc;
    } catch (hata) {
      this.sorunEkle('beklenti', `${ad}: ${(hata as Error).message.split('\n')[0] ?? ''}`);
      await this.kaydet(ad, once);
      return undefined;
    }
  }

  private async kaydet(ad: string, once: number) {
    const sorunlu = this.sorunlar.length > once;
    this.adimlar.push({ ad, sonuc: sorunlu ? 'sorun' : 'ok' });
    if (!sorunlu) return;
    const yol = this.info.outputPath(
      `${String(this.adimlar.length)}-${ad.replace(/\W+/g, '-')}.png`,
    );
    await this.sayfa.screenshot({ path: yol }).catch(() => undefined);
    const son = this.sorunlar[this.sorunlar.length - 1];
    if (son !== undefined && son.ekran === undefined) son.ekran = yol;
  }

  /** Beklenen bir koşulu denetler; tutmazsa sorun yazar, senaryo durmaz. */
  kontrol(ad: string, tamam: boolean, ayrinti = '') {
    this.adimlar.push({ ad, sonuc: tamam ? 'ok' : 'sorun' });
    if (!tamam) this.sorunEkle('beklenti', `${ad}${ayrinti === '' ? '' : ` — ${ayrinti}`}`);
  }

  /** Ekranda şu an görünen uyarı ve alan hataları. */
  async mesajlar(): Promise<string[]> {
    await this.sayfa.waitForTimeout(900);
    const bulunan = await this.sayfa
      .locator('[role=alert], .mantine-InputWrapper-error, .mantine-Notification-root')
      .allInnerTexts();
    return bulunan
      .map((metin) => metin.replace(/\s+/g, ' ').trim())
      .filter((metin) => metin !== '');
  }

  /** GET isteği; oturumdaki çerezlerle gateway üzerinden. */
  async api<T>(yol: string): Promise<T> {
    const yanit = await this.baglam.request.get(`${GATEWAY}${yol}`);
    if (!yanit.ok()) throw new Error(`${String(yanit.status())} GET ${yol}`);
    return (await yanit.json()) as T;
  }
}

/** Genel ("tekrar deneyin" gibi) hata metni neden söylemediği için yetersiz sayılır. */
export const GENEL_MESAJ = /lütfen tekrar deneyin|işlem yapılamadı|beklenmeyen bir hata|problem\./i;

export type Gorunum = { width: number; height: number; hasTouch?: boolean };

export const test = base.extend<{
  giris: (rol: string, ilkSifre?: string, gorunum?: Gorunum) => Promise<Robot>;
}>({
  giris: async ({ browser }, use, info) => {
    const robotlar: Robot[] = [];
    await use(async (rol, ilkSifre, gorunum) => {
      const kullanici = KULLANICILAR[rol];
      const sifre = OZEL_SIFRE[rol] ?? process.env.ROBOT_SIFRE;
      if (kullanici === undefined || sifre === undefined) {
        throw new Error(`"${rol}" için kullanıcı ya da şifre tanımlı değil (robot/README.md)`);
      }
      const baglam = await browser.newContext({
        locale: 'tr-TR',
        viewport: gorunum ?? { width: 1280, height: 900 },
        hasTouch: gorunum?.hasTouch ?? false,
      });
      const sayfa = await baglam.newPage();
      await sayfa.goto(`${GATEWAY}/oauth2/authorization/keycloak`);
      await sayfa.fill('#username', kullanici);
      await sayfa.fill('#password', ilkSifre ?? sifre);
      await sayfa.click('#kc-login');
      if (ilkSifre !== undefined) {
        // Yöneticinin verdiği geçici şifreyle ilk giriş: Keycloak yeni şifre ister.
        await sayfa.locator('#password-new').waitFor({ timeout: 20_000 });
        await sayfa.fill('#password-new', sifre);
        await sayfa.fill('#password-confirm', sifre);
        await sayfa.locator('input[type=submit], button[type=submit]').first().click();
      }
      await sayfa.waitForURL(/5173/, { timeout: 20_000 });
      await sayfa.waitForLoadState('networkidle');
      const robot = new Robot(sayfa, baglam, info, rol);
      robotlar.push(robot);
      return robot;
    });
    const sorunlar = robotlar.flatMap((r) => r.sorunlar.map((s) => ({ rol: r.rol, ...s })));
    const adimlar = robotlar.flatMap((r) => r.adimlar.map((a) => ({ rol: r.rol, ...a })));
    await info.attach('robot-sonuc', {
      body: JSON.stringify({ sorunlar, adimlar }),
      contentType: 'application/json',
    });
    for (const robot of robotlar) await robot.baglam.close();
    expect(sorunlar, 'Robot sorun buldu (rapor/rapor.html)').toEqual([]);
  },
});

export { expect };
