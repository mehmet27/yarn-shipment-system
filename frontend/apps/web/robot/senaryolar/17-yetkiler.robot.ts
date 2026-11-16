import { expect, rolTanimli, test } from '../support/robot';

const DENETIM_IZNI = 'Denetim kaydını görür';

test('Yetkiler: yönetici rolün iznini değiştirir; açık oturum yeniden giriş istemeden yeni izinle çalışır', async ({
  giris,
}) => {
  test.skip(!rolTanimli('admin') || !rolTanimli('genel-mudur'), 'admin ya da genel-mudur yok');
  const admin = await giris('admin');
  const genel = await giris('genel-mudur');
  genel.beklenen(/403 GET/);

  await admin.adim('yönetici rolü ve yalnızca yöneticide olan izinler kilitli', async () => {
    await admin.sayfa.goto('/panel/yetkiler');
    await admin.sayfa.getByRole('tab', { name: 'Yönetici' }).click();
    await expect(admin.sayfa.getByText('Yönetici rolünün yetkileri değiştirilemez.')).toBeVisible();
    await expect(admin.sayfa.getByRole('checkbox', { name: 'Yetkiler ekranı' })).toBeDisabled();
    await expect(admin.sayfa.getByRole('button', { name: 'Kaydet' })).toHaveCount(0);
    await admin.sayfa.getByRole('tab', { name: 'Müdür', exact: true }).click();
    await expect(admin.sayfa.getByRole('checkbox', { name: 'Kullanıcılar ekranı' })).toBeDisabled();
    await expect(admin.sayfa.getByText('Yalnızca yönetici')).toHaveCount(3);
  });

  await admin.adim('genel müdür başlangıçta denetim kaydını açamaz (403)', async () => {
    await genel.sayfa.goto('/panel/denetim');
    await genel.sayfa.waitForURL(/\/403$/);
  });

  await admin.adim('yönetici genel müdüre denetim izni verir ve kaydeder', async () => {
    await admin.sayfa.goto('/panel/yetkiler');
    await admin.sayfa.getByRole('tab', { name: 'Genel müdür' }).click();
    const kutu = admin.sayfa.getByRole('checkbox', { name: DENETIM_IZNI });
    await expect(kutu).not.toBeChecked();
    await kutu.check();
    await admin.sayfa.getByRole('button', { name: 'Kaydet' }).click();
    await expect(admin.sayfa.getByText('Yetkiler kaydedildi.')).toBeVisible();
  });

  await admin.adim(
    'genel müdür aynı oturumla denetim kaydını açar (yeniden giriş yok)',
    async () => {
      await genel.sayfa.goto('/panel/denetim');
      await genel.sayfa.waitForLoadState('networkidle');
      admin.kontrol(
        'genel müdür /panel/denetim açabilmeli',
        new URL(genel.sayfa.url()).pathname === '/panel/denetim',
        `adres: ${genel.sayfa.url()}`,
      );
    },
  );

  await admin.adim('varsayılana dön: izin geri alınır, genel müdür yine 403 görür', async () => {
    await admin.sayfa.goto('/panel/yetkiler');
    await admin.sayfa.getByRole('tab', { name: 'Genel müdür' }).click();
    await expect(admin.sayfa.getByRole('checkbox', { name: DENETIM_IZNI })).toBeChecked();
    await admin.sayfa.getByRole('button', { name: 'Varsayılana dön' }).click();
    await expect(admin.sayfa.getByText('Varsayılan yetkiler geri yüklendi.')).toBeVisible();
    await genel.sayfa.goto('/panel/denetim');
    await genel.sayfa.waitForURL(/\/403$/);
  });
});
