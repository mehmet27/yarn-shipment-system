import { API_SURESI_MS, SAYFA_SURESI_MS } from '../kurallar/hesap';
import { IZINLI } from '../kurallar/yetki';
import { expect, rolTanimli, test } from '../support/robot';
import { damga } from '../support/yardimci';

type Ozet = { shipments: Record<string, number> };
type Stok = { rows: { yarnType: string; stockKg: string }[] };

for (const rol of ['depo', 'kalite', 'mudur', 'admin'] as const) {
  test(`Erişilebilirlik ve hız: ${rol} ekranlarında etiketsiz öğe, başlık, sayfa ve API süresi`, async ({
    giris,
  }) => {
    test.skip(!rolTanimli(rol), `${rol} için kullanıcı/şifre tanımlı değil`);
    const robot = await giris(rol);
    const yavasApi: string[] = [];
    robot.sayfa.on('requestfinished', async (istek) => {
      const zaman = istek.timing();
      const sure = zaman.responseEnd - zaman.startTime;
      if (istek.url().includes('/api/') && sure > API_SURESI_MS)
        yavasApi.push(
          `${istek.method()} ${new URL(istek.url()).pathname} ${String(Math.round(sure))} ms`,
        );
    });
    for (const yol of IZINLI[rol] ?? []) {
      await robot.adim(`${yol}: erişilebilirlik ve hız`, async () => {
        const baslangic = Date.now();
        await robot.sayfa.goto(yol);
        await robot.sayfa.waitForLoadState('networkidle');
        const sure = Date.now() - baslangic;
        robot.kontrol(
          `${yol}: sayfa ${String(SAYFA_SURESI_MS)} ms içinde yüklenmeli`,
          sure <= SAYFA_SURESI_MS,
          `${String(sure)} ms`,
        );
        const bulgu = await robot.sayfa.evaluate(() => {
          const adsiz = (e: Element) => {
            const etiket =
              e.getAttribute('aria-label') ??
              e.getAttribute('aria-labelledby') ??
              (e.textContent ?? '').trim() ??
              '';
            return etiket === '' && e.getAttribute('title') === null;
          };
          const sorunlar: string[] = [];
          for (const e of document.querySelectorAll('button, [role=button], a')) {
            if (adsiz(e) && !e.querySelector('img[alt]:not([alt=""])'))
              sorunlar.push(`adsız ${e.tagName.toLowerCase()}: ${e.outerHTML.slice(0, 70)}`);
          }
          for (const e of document.querySelectorAll('input:not([type=hidden]), textarea, select')) {
            const kimlik = e.id;
            const etiketli =
              (kimlik !== '' && document.querySelector(`label[for="${kimlik}"]`) !== null) ||
              e.getAttribute('aria-label') !== null ||
              e.getAttribute('aria-labelledby') !== null ||
              e.closest('label') !== null;
            if (!etiketli) sorunlar.push(`etiketsiz alan: ${e.outerHTML.slice(0, 70)}`);
          }
          for (const e of document.querySelectorAll('img'))
            if (e.getAttribute('alt') === null)
              sorunlar.push(`alt yok: ${e.outerHTML.slice(0, 60)}`);
          if (document.documentElement.lang === '') sorunlar.push('html lang yok');
          if (document.title.trim() === '') sorunlar.push('sayfa başlığı boş');
          if (document.querySelectorAll('h1').length !== 1)
            sorunlar.push(`h1 sayısı ${String(document.querySelectorAll('h1').length)} (1 olmalı)`);
          return sorunlar.slice(0, 6);
        });
        robot.kontrol(
          `${yol}: erişilebilirlik (etiket, alt, lang, başlık)`,
          bulgu.length === 0,
          bulgu.join(' | '),
        );
      });
    }
    robot.kontrol(
      'yavaş API çağrısı olmamalı',
      yavasApi.length === 0,
      yavasApi.slice(0, 4).join(', '),
    );
  });
}

test('Dayanıklılık: yenileme, geri/ileri ve çift tıklama veri tekrarlamaz', async ({ giris }) => {
  const depo = await giris('depo');
  const mudur = await giris('mudur');
  for (const r of [depo, mudur]) r.beklenen(/4\d\d (POST|PUT) /);

  await depo.adim('her ekranda F5 ve geri/ileri sayfayı bozmaz', async () => {
    for (const yol of ['/panel', '/panel/sevkiyat', '/panel/merkez-stok']) {
      await depo.sayfa.goto(yol);
      await depo.sayfa.reload();
      await depo.sayfa.waitForLoadState('networkidle');
      const metin = await depo.sayfa.innerText('body');
      depo.kontrol(`${yol}: yenileme sonrası hatasız`, !/Beklenmeyen bir hata/.test(metin));
    }
    await depo.sayfa.goBack();
    await depo.sayfa.goForward();
    depo.kontrol(
      'geri/ileri sonrası sayfa hatasız',
      !/Beklenmeyen bir hata/.test(await depo.sayfa.innerText('body')),
    );
  });

  const iplik = damga();
  await depo.adim('iplik girişi: çift tıklama tek kayıt oluşturur', async () => {
    const once = await depo.api<Stok>('/api/v1/central-stock');
    await depo.sayfa.goto('/panel/merkez-stok');
    await depo.sayfa.getByRole('tab', { name: 'İplik girişi' }).click();
    await depo.sayfa
      .getByLabel(/İplik cinsi/)
      .first()
      .fill(iplik);
    await depo.sayfa.getByLabel(/Gelen net kg/).fill('10');
    await depo.sayfa.getByRole('button', { name: 'İplik girişini kaydet' }).dblclick();
    await depo.sayfa.waitForTimeout(2000);
    const sonra = await depo.api<Stok>('/api/v1/central-stock');
    const fark =
      Number(sonra.rows.find((r) => r.yarnType === iplik)?.stockKg ?? 0) -
      Number(once.rows.find((r) => r.yarnType === iplik)?.stockKg ?? 0);
    depo.kontrol(
      'çift tıklama yalnızca 10 kg eklemeli (20 kg değil)',
      Math.abs(fark - 10) < 0.001,
      `${String(fark)} kg eklendi`,
    );
  });

  await mudur.adim('yeni sevkiyat: çift tıklama tek taslak oluşturur', async () => {
    const once = await mudur.api<Ozet>('/api/v1/reports/dashboard');
    await mudur.sayfa.goto('/panel/sevkiyat/yeni');
    await mudur.sayfa
      .getByLabel(/Alıcı fasoncu/)
      .first()
      .click();
    await mudur.sayfa.locator('[role=option]:visible').first().click();
    await mudur.sayfa.getByLabel(/Fire oranı/).fill('3');
    await mudur.sayfa
      .getByLabel(/serbest metin/i)
      .first()
      .fill(iplik);
    await mudur.sayfa.getByLabel(/Net kg 1/).fill('10');
    await mudur.sayfa.getByRole('button', { name: 'Taslak oluştur' }).dblclick();
    await mudur.sayfa.waitForTimeout(2500);
    const sonra = await mudur.api<Ozet>('/api/v1/reports/dashboard');
    depoYok(mudur, (sonra.shipments.total ?? 0) - (once.shipments.total ?? 0));
  });
});

function depoYok(robot: { kontrol: (a: string, b: boolean, c?: string) => void }, fark: number) {
  robot.kontrol('çift tıklama tek sevkiyat açmalı', fark === 1, `${String(fark)} sevkiyat açıldı`);
}

test('Dayanıklılık: oturum kapanınca (çerezler silinince) uygulama anlaşılır şekilde giriş ister', async ({
  giris,
}) => {
  const mudur = await giris('mudur');
  mudur.beklenen(/401 /);
  await mudur.adim('oturumu düşür ve bir işlem yap', async () => {
    await mudur.sayfa.goto('/panel/sevkiyat');
    await mudur.baglam.clearCookies();
    await mudur.sayfa
      .getByRole('link', { name: /SV-/ })
      .first()
      .click()
      .catch(() => undefined);
    await mudur.sayfa.goto('/panel/raporlar');
    await mudur.sayfa.waitForLoadState('networkidle');
    await mudur.sayfa.waitForTimeout(1500);
    const metin = await mudur.sayfa.innerText('body');
    const girisSayfasi = mudur.sayfa.url().includes(':8180') || /Oturum aç|giriş yap/i.test(metin);
    mudur.kontrol('oturum düşünce giriş sayfasına yönlenmeli', girisSayfasi, mudur.sayfa.url());
    mudur.kontrol(
      'boş ya da hata ekranı kalmamalı',
      !/Beklenmeyen bir hata/.test(metin) && metin.trim() !== '',
    );
    await expect(mudur.sayfa.locator('body')).not.toContainText(/undefined|\[object Object\]/);
  });
});
