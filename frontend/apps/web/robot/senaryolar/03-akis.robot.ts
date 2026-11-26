import { expect, GENEL_MESAJ, test } from '../support/robot';
import type { Robot } from '../support/robot';
import {
  kaydetOnayla,
  merkezKumasDoldur,
  fiyatYaz,
  iplikTanimla,
  tedarikciTanimla,
  kumasFormuDoldur,
  kumasGir,
  oranYaz,
  sevkiyatIpligiSec,
  tartiyiOnayla,
} from '../support/akis';
import { damga, iplikGirisiDoldur, resim, sec } from '../support/yardimci';

type FireDosyasi = { fireRate: string; sentKg: string; fabricKg: string; status: string };
type Sevkiyat = { fireRate: string; status: string };

async function sekme(robot: Robot, ad: string) {
  // Adım çubuğunda da aynı adlar yazar (Çuvallar, Kumaş girişi); sekme rolüyle seçilir.
  await robot.sayfa.getByRole('tab', { name: ad, exact: true }).first().click();
  await robot.sayfa.waitForTimeout(600);
}

test('Akış: iplik girişinden kumaş, fire ve kalite itirazına tüm sevkiyat süreci', async ({
  giris,
}) => {
  const iplik = damga();
  const tedarikci = damga();
  const depo = await giris('depo');
  const kalite = await giris('kalite');
  const mudur = await giris('mudur');
  const fasonKalite = await giris('fason-kalite');
  // Bu senaryo reddedilen istekleri bilerek dener; 4xx beklenir, 5xx ve konsol hataları sorundur.
  for (const r of [depo, kalite, mudur, fasonKalite])
    r.beklenen(/4\d\d (POST|PUT|DELETE) /, /404 GET \/api\/v1\/shipments\/[\w-]+\/fire-settlement/);

  await mudur.adim(
    "müdür: ipliği Tanımlar'a ekler (stok girişi yalnızca tanımlı iplikle)",
    async () => {
      await iplikTanimla(mudur, iplik);
      await tedarikciTanimla(mudur, tedarikci);
    },
  );

  await depo.adim('depo: merkeze iplik girişi (listeden seçim)', async () => {
    await depo.sayfa.goto('/panel/merkez-stok');
    await depo.sayfa.getByRole('tab', { name: 'İplik girişi' }).click();
    await iplikGirisiDoldur(depo.sayfa, { iplik, tedarikci, kg: '200' });
    await kaydetOnayla(depo.sayfa, 'İplik girişini kaydet');
    await depo.sayfa.getByText('İplik girişi kaydedildi.').waitFor();
    await depo.sayfa.getByRole('tab', { name: 'Eldeki iplik' }).click();
    await depo.sayfa.getByText(iplik).first().waitFor();
  });

  await kalite.adim('kalite: stoktaki iplikle merkez kumaş üretimi', async () => {
    await kalite.sayfa.goto('/panel/merkez-stok');
    await kalite.sayfa.getByRole('tab', { name: 'Kumaş girişi' }).click();
    await merkezKumasDoldur(kalite.sayfa, { kumas: 'Süprem', iplik, kg: '50', fire: '3' });
    await kaydetOnayla(kalite.sayfa, 'Kumaşı kaydet');
    await kalite.sayfa.getByText('Kumaş girişi kaydedildi.').waitFor();
    await expect(kalite.sayfa.getByRole('combobox', { name: /^Kumaş cinsi/ })).toHaveValue('');
    await kalite.sayfa.getByRole('tab', { name: 'Eldeki iplik' }).click();
    await kalite.sayfa.getByText('Merkez kumaş üretimi').first().waitFor();
    const hareketler = await kalite.sayfa.innerText('main');
    kalite.kontrol(
      'yeterli stokla üretim "Son hareketler"de görünmeli',
      hareketler.includes(iplik),
    );
  });

  await kalite.adim('kalite: stoktan fazla üretim reddedilmeli ve nedeni söylenmeli', async () => {
    // Önceki adım "Eldeki iplik" sekmesinde bitti; form sekmesine dönülür, başarılı kayıttan sonra form boşalmıştır.
    await kalite.sayfa.getByRole('tab', { name: 'Kumaş girişi' }).click();
    await merkezKumasDoldur(kalite.sayfa, { kumas: 'Süprem', iplik, kg: '5000', fire: '3' });
    await kaydetOnayla(kalite.sayfa, 'Kumaşı kaydet');
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
    await sevkiyatIpligiSec(mudur.sayfa, iplik);
    await mudur.sayfa.getByLabel(/Net kg 1/).fill('100');
    await fiyatYaz(mudur.sayfa);
    await mudur.sayfa.getByRole('button', { name: 'Taslak oluştur' }).click();
    await mudur.sayfa.waitForURL(/sevkiyat\/[0-9a-f-]{30,}$/);
    sevkiyatId = mudur.sayfa.url().split('/').pop() ?? '';
  });

  await mudur.adim('müdür: taslak sevkiyatta itiraz kanıtı yüklenemez', async () => {
    await mudur.sayfa.goto(`/panel/sevkiyat/${sevkiyatId}`);
    await mudur.sayfa.getByText('Kilolar', { exact: true }).first().waitFor();
    await expect(mudur.sayfa.getByText('Yüklenecek belge ne?')).toHaveCount(0);
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

  await depo.adim('depo: Kilolar sekmesinden sipariş formunu PDF indirir', async () => {
    await depo.sayfa.goto(`/panel/sevkiyat/${sevkiyatId}`);
    await sekme(depo, 'Kilolar');
    const indirme = depo.sayfa.waitForEvent('download');
    await depo.sayfa.getByRole('button', { name: 'Sipariş formu PDF' }).click();
    expect((await indirme).suggestedFilename()).toMatch(/\.pdf$/i);
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
    await depo.sayfa
      .getByText(/Fotoğraf seçildi/)
      .first()
      .waitFor();
    await depo.sayfa.getByRole('button', { name: 'İleri' }).click();
    await tartiyiOnayla(depo);
    const mesajlar = await depo.mesajlar();
    return { ad, mesajlar };
  };

  await depo.adim('depo: fotoğraf seçilince onaylanana kadar sisteme yüklenmez', async () => {
    let yukleme = 0;
    depo.sayfa.on('request', (istek) => {
      if (istek.method() === 'POST' && /\/api\/v1\/evidence\/uploads$/.test(istek.url())) {
        yukleme += 1;
      }
    });
    await depo.sayfa.goto(`/panel/sevkiyat/${sevkiyatId}/cikis`);
    await depo.sayfa.getByLabel(/Dolu araç tartısı/).fill('103,5');
    await depo.sayfa.getByLabel(/Boş araç tartısı/).fill('3,5');
    await depo.sayfa.getByLabel(/Çuval sayısı/).fill('2');
    await depo.sayfa
      .locator('input[type=file]')
      .first()
      .setInputFiles({
        name: 'fis.jpg',
        mimeType: 'image/jpeg',
        buffer: await resim(depo.sayfa, 4),
      });
    await depo.sayfa
      .getByText(/Fotoğraf seçildi/)
      .first()
      .waitFor();
    await depo.sayfa.waitForTimeout(1000);
    depo.kontrol(
      'onaydan önce fotoğraf yüklenmemeli',
      yukleme === 0,
      `yükleme: ${String(yukleme)}`,
    );
  });

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
      'durum Fasoncuya yolda',
      /fasoncuya yolda/i.test(await depo.sayfa.innerText('main')),
    );
  });

  await depo.adim(
    'depo: yola çıkmış sevkiyatta kantar fişi yenilenemez (ekranda yok, sunucu 409)',
    async () => {
      await depo.sayfa.goto(`/panel/sevkiyat/${sevkiyatId}`);
      await depo.sayfa.waitForLoadState('networkidle');
      depo.kontrol(
        '"Kantar fişini yenile" bölümü görünmemeli',
        (await depo.sayfa.getByText('Kantar fişini yenile').count()) === 0,
      );
      const kod = await depo.sayfa.evaluate(async (id) => {
        const cerez = document.cookie.split('; ').find((c) => c.startsWith('XSRF-TOKEN='));
        const yanit = await fetch('/api/v1/evidence/uploads', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Idempotency-Key': crypto.randomUUID(),
            'X-XSRF-TOKEN': decodeURIComponent(cerez?.split('=')[1] ?? ''),
          },
          body: JSON.stringify({
            shipmentId: id,
            evidenceType: 'DISPATCH_SCALE',
            mimeType: 'image/jpeg',
            sizeBytes: 1000,
          }),
        });
        return `${String(yanit.status)} ${((await yanit.json()) as { code?: string }).code ?? ''}`;
      }, sevkiyatId);
      depo.kontrol(
        'yola çıkmış sevkiyatta fiş yenileme 409 SHIPMENT_DEPARTED dönmeli',
        kod === '409 SHIPMENT_DEPARTED',
        kod,
      );
    },
  );

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
    await expect(mudur.sayfa.getByText(/irsaliye numarasını yazın\./)).toBeVisible();
    await expect(mudur.sayfa.getByText(/sonradan değiştirilemez/)).toBeVisible();
    await mudur.sayfa.getByLabel(/İrsaliye no/).fill(`${iplik}-IRS`);
    await expect(mudur.sayfa.getByText('Tamamlamak için irsaliye dosyasını')).toHaveCount(0);
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

  await fasonKalite.adim(
    'fasoncu kalite: fasoncudaki iplik için oran kutusu çıkar, canlı toplam yazar',
    async () => {
      const sayfa = fasonKalite.sayfa;
      await kumasFormuDoldur(fasonKalite, sevkiyatId, '50', `${iplik}-K0`);
      await expect(sayfa.getByLabel(/^Oran: /).first()).toBeVisible();
      await expect(sayfa.getByTestId('fabric-ratio-total')).toHaveText(
        'Toplam: %0 (tam %100 olmalı)',
      );
      await oranYaz(fasonKalite);
      await expect(sayfa.getByTestId('fabric-ratio-total')).toHaveText('Toplam: %100');
    },
  );

  await fasonKalite.adim(
    'fasoncu kalite: fasoncudaki ipliği aşan kumaş girişi kontrolde uyarılır, onay sorusu çıkmaz, kayıt yazılmaz',
    async () => {
      await kumasGir(fasonKalite, sevkiyatId, '150', `${iplik}-K1`, false);
      await expect(fasonKalite.sayfa.getByText('Fasoncuda yeterli iplik yok')).toBeVisible();
      await expect(fasonKalite.sayfa.getByText(/50 kg eksik/)).toBeVisible();
      fasonKalite.kontrol(
        'aşan kiloda "Evet, kaydet" sorusu çıkmamalı',
        (await fasonKalite.sayfa.getByRole('button', { name: 'Evet, kaydet' }).count()) === 0,
      );
      const fire = await fasonKalite.api<FireDosyasi>(
        `/api/v1/shipments/${sevkiyatId}/fire-settlement`,
      );
      fasonKalite.kontrol('hiç kumaş kaydolmamalı', Number(fire.fabricKg) === 0, fire.fabricKg);
    },
  );

  await fasonKalite.adim(
    'fasoncu kalite: kaydet önce özet ve onay sorar; Vazgeç kaydetmez, onaylanınca kayıt yazılır',
    async () => {
      const sayfa = fasonKalite.sayfa;
      await kumasFormuDoldur(fasonKalite, sevkiyatId, '90', `${iplik}-K2`);
      await oranYaz(fasonKalite);
      await sayfa.getByRole('button', { name: 'Kontrol et' }).click();
      await expect(sayfa.getByText('Kumaş için şu iplikler düşülecek')).toBeVisible();
      await expect(
        sayfa.getByText(/90 kg Süprem kumaş kaydedilecek\. Kayıt sonradan değiştirilemez\./),
      ).toBeVisible();
      await sayfa.getByRole('button', { name: 'Vazgeç' }).click();
      await expect(sayfa.getByRole('button', { name: 'Kontrol et' })).toBeVisible();
      const once = await fasonKalite.api<FireDosyasi>(
        `/api/v1/shipments/${sevkiyatId}/fire-settlement`,
      );
      fasonKalite.kontrol(
        'Vazgeç sonrası kumaş kaydolmamalı',
        Number(once.fabricKg) === 0,
        once.fabricKg,
      );
      await sayfa.getByRole('button', { name: 'Kontrol et' }).click();
      await sayfa.getByRole('button', { name: 'Evet, kaydet' }).click();
      await expect(sayfa.getByText('Kumaş girişi kaydedildi.')).toBeVisible();
    },
  );

  await fasonKalite.adim(
    'fasoncu kalite: kayıttan sonra kumaş kartı iplik payını yazar, fire dosyası sayıları tutar',
    async () => {
      await expect(fasonKalite.sayfa.getByText(/%100/).first()).toBeVisible();
      const fire = await fasonKalite.api<FireDosyasi>(
        `/api/v1/shipments/${sevkiyatId}/fire-settlement`,
      );
      fasonKalite.kontrol('kumaş toplamı 90 kg', Number(fire.fabricKg) === 90, fire.fabricKg);
      fasonKalite.kontrol(
        'kumaş girişi sonrası dosya faturalanmaya hazır',
        fire.status === 'TO_INVOICE',
        fire.status,
      );
    },
  );

  await mudur.adim('müdür: kumaş girişi yapamaz (formu görmez)', async () => {
    await mudur.sayfa.goto(`/panel/sevkiyat/${sevkiyatId}`);
    await sekme(mudur, 'Kumaş girişi');
    mudur.kontrol(
      'müdürde kumaş girişi formu yok',
      (await mudur.sayfa.getByRole('button', { name: 'Kontrol et' }).count()) === 0,
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

  await mudur.adim(
    'müdür: kalite itirazını görür; yalnız haklı / haksız kararı verebilir, atama ve sorumluluk kararı yok',
    async () => {
      await mudur.sayfa.goto('/panel/uyusmazliklar');
      await mudur.sayfa.locator('main a[href*="uyusmazliklar/"]').first().click();
      await expect(mudur.sayfa.getByRole('heading', { name: 'Karar ver' })).toBeVisible();
      mudur.kontrol(
        'müdürde "Bana ata" düğmesi olmamalı',
        (await mudur.sayfa.getByRole('button', { name: 'Bana ata' }).count()) === 0,
      );
      await mudur.sayfa
        .getByLabel(/^Karar/)
        .first()
        .click();
      const secenekler = await mudur.sayfa.locator('[role=option]:visible').allInnerTexts();
      mudur.kontrol(
        'seçenekler yalnız "Haklı" ve "Haksız" olmalı (fatura bu ekranda girilmez)',
        secenekler.length > 0 &&
          secenekler.every((s) => /^(Haklı|Haksız)$/.test(s.trim())) &&
          secenekler.some((s) => s.trim() === 'Haksız'),
        secenekler.join(' | '),
      );
      await mudur.sayfa.keyboard.press('Escape');
    },
  );

  await kalite.adim('kalite: üstlenir ama karar veremez', async () => {
    await kalite.sayfa.goto('/panel/uyusmazliklar');
    await kalite.sayfa.locator('main a[href*="uyusmazliklar/"]').first().click();
    await kalite.sayfa.waitForLoadState('networkidle');
    const ata = kalite.sayfa.getByRole('button', { name: 'Bana ata' });
    // Kalitenin kendi açtığı itiraz ona atanmış gelebilir; o zaman düğme yerine "sizde" yazar.
    if ((await ata.count()) > 0) await ata.click();
    await expect(kalite.sayfa.getByText('Bu uyuşmazlık sizde.')).toBeVisible();
    kalite.kontrol(
      'atandıktan sonra "Bana ata" düğmesi görünmemeli',
      (await kalite.sayfa.getByRole('button', { name: 'Bana ata' }).count()) === 0,
    );
    kalite.kontrol(
      'kalitede "Kontrol iste" düğmesi ve "Talep notu" alanı olmamalı (laboratuvar adımı kaldırıldı)',
      (await kalite.sayfa.getByRole('button', { name: 'Kontrol iste' }).count()) === 0 &&
        (await kalite.sayfa.getByLabel(/Talep notu/).count()) === 0,
    );
    kalite.kontrol(
      'kalitede "Karar ver" bölümü olmamalı (karar yalnız müdürde)',
      (await kalite.sayfa.getByRole('heading', { name: 'Karar ver' }).count()) === 0,
    );
  });
});
