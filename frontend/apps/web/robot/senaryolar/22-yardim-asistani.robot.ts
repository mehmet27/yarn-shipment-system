import { expect, test } from '../support/robot';

/**
 * Yardım asistanı: sağ alttaki "Yardım" balonu yalnızca sunucuda API anahtarı tanımlıysa görünür. Bu senaryo gerçek bir soru
 * sormaz (anahtar harcanmaz); balonun görünürlüğünün sunucu durumuyla tutarlı olduğunu, pencerenin açılıp kapandığını ve
 * hiçbir rolde sayfa düzenini bozmadığını denetler.
 */
for (const rol of ['depo', 'kalite', 'mudur', 'genel-mudur']) {
  test(`Yardım asistanı: ${rol} için balon sunucu durumuyla tutarlı, pencere açılıp kapanır`, async ({
    giris,
  }) => {
    const robot = await giris(rol);
    await robot.sayfa.goto('/panel');
    await robot.sayfa.waitForLoadState('networkidle');
    const durum = await robot.api<{ available: boolean }>('/api/v1/assistant/status');
    const balon = robot.sayfa.getByRole('button', { name: 'Yardım', exact: true });

    if (!durum.available) {
      await robot.adim('anahtar yok: balon görünmez', async () => {
        await expect(balon).toHaveCount(0);
      });
      return;
    }
    await robot.adim('anahtar var: balon sağ altta görünür', async () => {
      await expect(balon).toBeVisible();
      const kutu = await balon.boundingBox();
      const pencere = robot.sayfa.viewportSize();
      robot.kontrol(
        'balon ekranın sağ alt köşesinde olmalı',
        kutu !== null &&
          pencere !== null &&
          kutu.x + kutu.width > pencere.width / 2 &&
          kutu.y + kutu.height > pencere.height / 2,
      );
    });
    await robot.adim('balona basınca pencere açılır, karşılama ve uyarı görünür', async () => {
      await balon.click();
      const pencere = robot.sayfa.getByRole('dialog', { name: 'Yardım' });
      await expect(pencere).toBeVisible();
      await expect(
        pencere.getByText(/Uygulamayı nasıl kullanacağınızı bana sorabilirsiniz/),
      ).toBeVisible();
      await expect(pencere.getByText(/Yardım yalnızca bilgi verir/)).toBeVisible();
    });
    await robot.adim('soru yazılmadan "Gönder" pasiftir', async () => {
      const pencere = robot.sayfa.getByRole('dialog', { name: 'Yardım' });
      await expect(pencere.getByRole('button', { name: 'Gönder', exact: true })).toBeDisabled();
    });
    await robot.adim('"Yardımı kapat" pencereyi kapatır', async () => {
      await robot.sayfa.getByRole('button', { name: 'Yardımı kapat' }).click();
      await expect(robot.sayfa.getByRole('dialog', { name: 'Yardım' })).toHaveCount(0);
    });
  });
}
