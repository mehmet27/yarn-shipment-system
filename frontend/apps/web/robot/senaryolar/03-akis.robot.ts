import { expect, GENEL_MESAJ, test } from '../support/robot';
import type { Robot } from '../support/robot';
import { damga, resim, sec } from '../support/yardimci';

type FireDosyasi = { fireRate: string; sentKg: string; fabricKg: string; status: string };
type Sevkiyat = { fireRate: string; status: string };

async function sekme(robot: Robot, ad: string) {
  await robot.sayfa.getByText(ad, { exact: true }).first().click();
  await robot.sayfa.waitForTimeout(600);
}

test('Akış: iplik girişinden kumaş, fire ve kalite itirazına tüm sevkiyat süreci', async ({
  giris,
}) => {
  const iplik = damga();
  const depo = await giris('depo');
  const kalite = await giris('kalite');
  const mudur = await giris('mudur');
  // Bu senaryo reddedilen istekleri bilerek dener; 4xx beklenir, 5xx ve konsol hataları sorundur.
  for (const r of [depo, kalite, mudur])
    r.beklenen(/4\d\d (POST|PUT|DELETE) /, /404 GET \/api\/v1\/shipments\/[\w-]+\/fire-settlement/);

  await depo.adim('depo: merkeze iplik girişi', async () => {
    await depo.sayfa.goto('/panel/merkez-stok');
    await depo.sayfa.getByRole('tab', { name: 'İplik girişi' }).click();
    await depo.sayfa
      .getByLabel(/İplik cinsi/)
      .first()
      .fill(iplik);
    await depo.sayfa.getByLabel(/Gelen net kg/).fill('200');
    await depo.sayfa.getByRole('button', { name: 'İplik girişini kaydet' }).click();
    await depo.sayfa.getByText('İplik girişi kaydedildi.').waitFor();
    await depo.sayfa.getByRole('tab', { name: 'Eldeki iplik' }).click();
    await depo.sayfa.getByText(iplik).first().waitFor();
  });

  await kalite.adim('kalite: stoktaki iplikle merkez kumaş üretimi', async () => {
    await kalite.sayfa.goto('/panel/merkez-stok');
    await kalite.sayfa.getByRole('tab', { name: 'Kumaş girişi' }).click();
    await kalite.sayfa.getByLabel(/Kumaş cinsi/).fill('Süprem');
    await kalite.sayfa.getByLabel(/İplik cinsi 1/).fill(iplik);
    await kalite.sayfa.getByLabel(/Kumaş kg 1/).fill('50');
    await kalite.sayfa.getByLabel(/Fire \(%\)/).fill('3');
    await kalite.sayfa.getByRole('button', { name: 'Kumaşı kaydet' }).click();
    await kalite.sayfa.getByText('Kumaş girişi kaydedildi.').waitFor();
    await expect(kalite.sayfa.getByLabel(/Kumaş cinsi/)).toHaveValue('');
    await kalite.sayfa.getByRole('tab', { name: 'Eldeki iplik' }).click();
    await kalite.sayfa.getByText('Merkez kumaş üretimi').first().waitFor();
    const hareketler = await kalite.sayfa.innerText('main');
    kalite.kontrol(
      'yeterli stokla üretim "Son hareketler"de görünmeli',
      hareketler.includes(iplik),
    );
  });

  await kalite.adim('kalite: stoktan fazla üretim reddedilmeli ve nedeni söylenmeli', async () => {
    // Başarılı kayıttan sonra form boşalır; alanlar yeniden doldurulur.
    await kalite.sayfa.getByLabel(/Kumaş cinsi/).fill('Süprem');
    await kalite.sayfa.getByLabel(/İplik cinsi 1/).fill(iplik);
    await kalite.sayfa.getByLabel(/Fire \(%\)/).fill('3');
    await kalite.sayfa.getByLabel(/Kumaş kg 1/).fill('5000');
    await kalite.sayfa.getByRole('button', { name: 'Kumaşı kaydet' }).click();
    const mesajlar = await kalite.mesajlar();
    kalite.kontrol(
      'stok yetersizliği mesajı',
      mesajlar.some((m) => /stok/i.test(m) && !GENEL_MESAJ.test(m)),
      mesajlar.join(' | '),
    );
  });

  let sevkiyatId = '';
  await mudur.adim('müdür: yeni sevkiyat taslağı (fire %3)', async () => {
    await mudur.sayfa.goto('/panel/sevkiyat/yeni');
    await sec(mudur.sayfa, /Alıcı fasoncu/, 'Başka Dokuma');
    await mudur.sayfa.getByLabel(/Fire oranı/).fill('3');
    await mudur.sayfa
      .getByLabel(/serbest metin/i)
      .first()
      .fill(iplik);
    await mudur.sayfa.getByLabel(/Net kg 1/).fill('100');
    await mudur.sayfa.getByRole('button', { name: 'Taslak oluştur' }).click();
    await mudur.sayfa.waitForURL(/sevkiyat\/[0-9a-f-]{30,}$/);
    sevkiyatId = mudur.sayfa.url().split('/').pop() ?? '';
  });

  await depo.adim('depo: iki çuval ekler (2 × 50 kg)', async () => {
    await depo.sayfa.goto(`/panel/sevkiyat/${sevkiyatId}`);
    await sekme(depo, 'Çuvallar');
    for (let i = 0; i < 2; i += 1) {
      await depo.sayfa.getByLabel(/Çuval kilosu/).fill('50');
      await depo.sayfa.getByRole('button', { name: 'Listeye ekle' }).click();
    }
    await depo.sayfa.getByRole('button', { name: /çuvalı ekle/ }).click();
    await depo.sayfa.getByText('Sevkiyattaki çuvallar (2)').waitFor();
  });

  const tarteGir = async (brut: string, dara: string, cuval: string, ad: string) => {
    await depo.sayfa.goto(`/panel/sevkiyat/${sevkiyatId}/cikis`);
    await depo.sayfa.getByLabel(/Dolu araç tartısı/).fill(brut);
    await depo.sayfa.getByLabel(/Boş araç tartısı/).fill(dara);
    await depo.sayfa.getByLabel(/Çuval sayısı/).fill(cuval);
    await depo.sayfa
      .locator('input[type=file]')
      .first()
      .setInputFiles({
        name: 'fis.jpg',
        mimeType: 'image/jpeg',
        buffer: await resim(depo.sayfa, 3),
      });
    await depo.sayfa.getByRole('button', { name: 'Fotoğrafı gönder' }).click();
    await depo.sayfa
      .getByText(/fotoğrafı kaydedildi/)
      .first()
      .waitFor();
    await depo.sayfa.getByRole('button', { name: 'Kaydet' }).click();
    const mesajlar = await depo.mesajlar();
    return { ad, mesajlar };
  };

  await depo.adim('depo: çuval sayısı tutmayan tartı nedenini söylemeli', async () => {
    const { mesajlar } = await tarteGir('103,5', '3,5', '3', 'çuval 3');
    depo.kontrol(
      'çuval sayısı uyarısı',
      mesajlar.some((m) => /çuval/i.test(m) && !/Lütfen tekrar deneyin$/.test(m)),
      mesajlar.join(' | '),
    );
  });

  await depo.adim('depo: doğru çıkış tartısı sevkiyatı yola çıkarır', async () => {
    await tarteGir('103,5', '3,5', '2', 'doğru');
    await depo.sayfa.waitForURL(new RegExp(`${sevkiyatId}$`), { timeout: 15_000 });
    depo.kontrol(
      'durum FASONCUYA YOLDA',
      /FASONCUYA YOLDA/.test(await depo.sayfa.innerText('main')),
    );
  });

  await mudur.adim('müdür: irsaliyeyle sevkiyatı tamamlar', async () => {
    await mudur.sayfa.goto(`/panel/sevkiyat/${sevkiyatId}`);
    await mudur.sayfa
      .locator('input[type=file]')
      .first()
      .setInputFiles({
        name: 'irs.jpg',
        mimeType: 'image/jpeg',
        buffer: await resim(mudur.sayfa, 5),
      });
    await mudur.sayfa.getByText('İrsaliye yüklendi').waitFor();
    await mudur.sayfa.getByLabel(/İrsaliye no/).fill(`${iplik}-IRS`);
    await mudur.sayfa.getByRole('button', { name: 'Sevkiyatı tamamla' }).click();
    await expect(mudur.sayfa.locator('main')).toContainText(/tamamlandı/i);
  });

  await mudur.adim('sayı: fire dosyası sevkiyattaki fire oranıyla açılmış olmalı', async () => {
    const sevkiyat = await mudur.api<Sevkiyat>(`/api/v1/shipments/${sevkiyatId}`);
    const fire = await mudur.api<FireDosyasi>(`/api/v1/shipments/${sevkiyatId}/fire-settlement`);
    mudur.kontrol(
      'fire dosyası oranı = sevkiyat oranı',
      Number(fire.fireRate) === Number(sevkiyat.fireRate),
      `${fire.fireRate} ≠ ${sevkiyat.fireRate}`,
    );
    mudur.kontrol('gönderilen iplik 100 kg', Number(fire.sentKg) === 100, fire.sentKg);
  });

  await mudur.adim('müdür: gönderilen ipliği aşan kumaş girişi reddedilmeli', async () => {
    await mudur.sayfa.goto(`/panel/sevkiyat/${sevkiyatId}`);
    await sekme(mudur, 'Kumaş girişi');
    await mudur.sayfa.getByLabel(/Kumaş cinsi/).fill('Süprem');
    await mudur.sayfa.getByLabel(/Kumaş net kilosu/).fill('150');
    await mudur.sayfa.getByLabel(/İrsaliye no/).fill(`${iplik}-K1`);
    await mudur.sayfa.getByRole('button', { name: 'Kumaş girişini kaydet' }).click();
    const mesajlar = await mudur.mesajlar();
    mudur.kontrol(
      'iplikten fazla kumaş nedeniyle reddedilmeli',
      mesajlar.some((m) => /iplik/i.test(m) && !GENEL_MESAJ.test(m)),
      mesajlar.join(' | '),
    );
  });

  await mudur.adim('müdür: makul kumaş girişi kaydolur, fire dosyası sayıları tutar', async () => {
    await mudur.sayfa.getByLabel(/Kumaş net kilosu/).fill('90');
    await mudur.sayfa.getByLabel(/İrsaliye no/).fill(`${iplik}-K2`);
    await mudur.sayfa.getByRole('button', { name: 'Kumaş girişini kaydet' }).click();
    await mudur.sayfa.waitForTimeout(1500);
    const fire = await mudur.api<FireDosyasi>(`/api/v1/shipments/${sevkiyatId}/fire-settlement`);
    mudur.kontrol('kumaş toplamı 90 kg', Number(fire.fabricKg) === 90, fire.fabricKg);
    mudur.kontrol(
      'kumaş girişi sonrası dosya faturalanmaya hazır',
      fire.status === 'TO_INVOICE',
      fire.status,
    );
  });

  await kalite.adim(
    'kalite: tamamlanmış sevkiyata kalite itirazı açar (sunucu hatası vermemeli)',
    async () => {
      await kalite.sayfa.goto(`/panel/sevkiyat/${sevkiyatId}`);
      await sekme(kalite, 'Kalite itirazı');
      await kalite.sayfa.getByRole('button', { name: 'Kalite itirazı aç' }).click();
      await kalite.sayfa.getByLabel(/İtiraz edilen kilo/).fill('10');
      await kalite.sayfa.getByLabel(/Sorunu anlatın/).fill('Robot: leke var');
      await kalite.sayfa
        .locator('input[type=file]')
        .last()
        .setInputFiles({
          name: 'leke.jpg',
          mimeType: 'image/jpeg',
          buffer: await resim(kalite.sayfa, 9),
        });
      await kalite.sayfa.getByText('1 fotoğraf eklendi.').waitFor();
      await kalite.sayfa.getByRole('button', { name: 'İtirazı gönder' }).click();
      await expect(kalite.sayfa.locator('main')).toContainText(/UY-SV-/);
    },
  );

  await kalite.adim('kalite: lab kontrolü beklerken karar verilememeli', async () => {
    await kalite.sayfa.goto('/panel/uyusmazliklar');
    await kalite.sayfa.locator('main a[href*="uyusmazliklar/"]').first().click();
    await kalite.sayfa.getByRole('button', { name: 'Bana ata' }).click();
    await kalite.sayfa.getByLabel(/Talep notu/).fill('Robot: numune alın');
    await kalite.sayfa.getByRole('button', { name: 'Kontrol iste' }).click();
    await kalite.sayfa.waitForTimeout(1200);
    await sec(kalite.sayfa, /^Karar/, 'Talep haksız');
    await kalite.sayfa.getByLabel(/^Not/).first().fill('Robot kararı');
    await kalite.sayfa.getByRole('button', { name: 'Karara bağla' }).click();
    const mesajlar = await kalite.mesajlar();
    kalite.kontrol(
      'bekleyen kontrol uyarısı',
      mesajlar.some((m) => /sonuc|sonuç|kontrol/i.test(m)),
      mesajlar.join(' | '),
    );
  });
});
