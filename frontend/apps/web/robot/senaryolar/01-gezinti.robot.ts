import { EKRANLAR, IZINLI } from '../kurallar/yetki';
import { rolTanimli, test } from '../support/robot';

for (const [rol, izinli] of Object.entries(IZINLI)) {
  test(`Gezinti: ${rol} tüm ekranları açar ve yetkisizlerde 403 görür`, async ({ giris }) => {
    test.skip(!rolTanimli(rol), `${rol} için kullanıcı/şifre tanımlı değil`);
    const robot = await giris(rol);
    for (const yol of EKRANLAR) {
      await robot.adim(`${yol} aç`, async () => {
        const yetkili = izinli.includes(yol);
        if (!yetkili) robot.beklenen(/403 GET/);
        await robot.sayfa.goto(yol);
        await robot.sayfa.waitForLoadState('networkidle');
        await robot.sayfa.waitForTimeout(500);
        const bulunan = new URL(robot.sayfa.url()).pathname;
        const metin = await robot.sayfa.innerText('body');
        robot.kontrol(
          `${yol}: ${yetkili ? 'açılmalı' : '403 vermeli'}`,
          yetkili
            ? bulunan === yol &&
                !/Beklenmeyen bir hata|Aradığınız sayfa bulunamadı|Bu sayfayı görme yetkiniz yok/.test(
                  metin,
                )
            : bulunan === '/403',
          `adres: ${bulunan}`,
        );
      });
    }
  });
}
