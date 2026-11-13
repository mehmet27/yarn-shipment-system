import { DOKUNMA_HEDEFI_PX } from '../kurallar/hesap';
import { IZINLI } from '../kurallar/yetki';
import { rolTanimli, test } from '../support/robot';

/** Telefon ve tablet genişlikleri (e2e testleriyle aynı, FE-UI-07). */
const CIHAZLAR = [
  { ad: 'telefon 360', width: 360, height: 780, hasTouch: true },
  { ad: 'tablet 768', width: 768, height: 1024, hasTouch: true },
] as const;
const ROLLER = ['depo', 'kalite', 'mudur'] as const;

for (const cihaz of CIHAZLAR) {
  for (const rol of ROLLER) {
    test(`Cihazlar: ${rol} ${cihaz.ad} genişlikte ekranlar taşmaz ve dokunma hedefleri yeterli`, async ({
      giris,
    }) => {
      test.skip(!rolTanimli(rol), `${rol} için kullanıcı/şifre tanımlı değil`);
      const robot = await giris(rol, undefined, cihaz);
      robot.beklenen(/4\d\d GET/);
      for (const yol of (IZINLI[rol] ?? []).filter((y) => !y.includes('yeni') || rol === 'mudur')) {
        await robot.adim(`${cihaz.ad} › ${yol}`, async () => {
          await robot.sayfa.goto(yol);
          await robot.sayfa.waitForLoadState('networkidle');
          await robot.sayfa.waitForTimeout(400);
          const olcum = await robot.sayfa.evaluate((enAz) => {
            const tasan =
              document.documentElement.scrollWidth - document.documentElement.clientWidth;
            const kucuk: string[] = [];
            for (const e of document.querySelectorAll(
              'main button, main a, header button, nav a',
            )) {
              const k = e.getBoundingClientRect();
              const gorunur =
                k.width > 0 && k.height > 0 && getComputedStyle(e).visibility !== 'hidden';
              if (
                gorunur &&
                (k.width < enAz || k.height < enAz) &&
                (e.textContent ?? '').trim() !== ''
              ) {
                kucuk.push(
                  `${(e.textContent ?? '').trim().slice(0, 24)} (${Math.round(k.width)}×${Math.round(k.height)})`,
                );
              }
            }
            return { tasan, kucuk: kucuk.slice(0, 5) };
          }, DOKUNMA_HEDEFI_PX);
          robot.kontrol(
            `${yol}: yatay taşma olmamalı`,
            olcum.tasan <= 1,
            `${String(olcum.tasan)} px taşıyor`,
          );
          robot.kontrol(
            `${yol}: dokunma hedefleri en az ${String(DOKUNMA_HEDEFI_PX)} px olmalı`,
            olcum.kucuk.length === 0,
            olcum.kucuk.join(', '),
          );
        });
      }
    });
  }
}
