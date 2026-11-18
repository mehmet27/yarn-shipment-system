import { cuvalEkle, iplikTanimla, kumasGir, tamamlanmisSevkiyat, taslakAc } from '../support/akis';
import { expect, test } from '../support/robot';
import { damga, iplikSec } from '../support/yardimci';

const UUID = /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/;

/**
 * Sadeleştirme: fabrika çalışanının gözüyle dil ve sıra düzeltmeleri. Menü başlığı "Menü" (Yönetim Paneli değil), depo ana
 * sayfasında iş listesi, müdür menüsünde iki başlık, çuvalsız tartı engeli, tartı formunda önce dara, fire dosyasında düz cümle, bakiyede kod yok,
 * kalitede belge türünü seçtirme, raporda kumaşı gelmemiş sevkiyatta fire yerine "—".
 */
test('Sadeleştirme: menü, depo iş listesi, çuval-önce, tartı sırası, fire dosyası, bakiye ve belge türü', async ({
  giris,
}) => {
  const depo = await giris('depo');
  const mudur = await giris('mudur');
  const kalite = await giris('kalite');
  const fasonKalite = await giris('fason-kalite');
  for (const r of [depo, mudur, kalite, fasonKalite]) r.beklenen(/4\d\d (POST|PUT|PATCH) /);
  const iplik = damga();

  await depo.adim('menü: başlık "Menü", "Yönetim Paneli" yazmaz', async () => {
    await depo.sayfa.goto('/panel');
    await depo.sayfa.waitForLoadState('networkidle');
    const metin = await depo.sayfa.innerText('body');
    depo.kontrol('yan menü başlığı "Menü" olmalı', /MENÜ/i.test(metin));
    depo.kontrol('"Yönetim Paneli" yazmamalı', !/yönetim paneli/i.test(metin));
  });

  await mudur.adim(
    'müdür menüsü: "Günlük işler" ve "Tanımlar ve ayarlar" başlıklarına ayrılır',
    async () => {
      await mudur.sayfa.goto('/panel');
      await mudur.sayfa.waitForLoadState('networkidle');
      const menu = await mudur.sayfa.locator('nav, aside, [class*="Navbar"]').first().innerText();
      mudur.kontrol(
        '"Günlük işler" başlığı olmalı',
        /GÜNLÜK İŞLER/i.test(menu),
        menu.slice(0, 200),
      );
      mudur.kontrol('"Tanımlar ve ayarlar" başlığı olmalı', /TANIMLAR VE AYARLAR/i.test(menu));
    },
  );
  await depo.adim('depo menüsü: tek liste, grup başlığı yok', async () => {
    const metin = await depo.sayfa.innerText('body');
    depo.kontrol('depoda "Günlük işler" başlığı yazmamalı', !/günlük işler/i.test(metin));
  });

  await mudur.adim('müdür: ipliği tanımlar, stok girişi için depo hazırlanır', async () => {
    await iplikTanimla(mudur, iplik);
  });
  await mudur.adim('aynı iplik adı ikinci kez eklenemez', async () => {
    await mudur.sayfa.goto('/panel/tanimlar');
    await mudur.sayfa.getByRole('tab', { name: 'İplikler', exact: true }).click();
    await mudur.sayfa.getByRole('button', { name: 'Ekle' }).click();
    const pencere = mudur.sayfa.getByRole('dialog');
    await pencere.getByLabel('Kod *', { exact: true }).fill(`${iplik}-B`);
    await pencere.getByLabel('İplik adı *', { exact: true }).fill(iplik.toLowerCase());
    await pencere.getByRole('button', { name: 'Kaydet' }).click();
    await expect(pencere.getByText('Bu adla kayıtlı bir iplik zaten var.')).toBeVisible();
    await mudur.sayfa.keyboard.press('Escape');
  });

  await depo.adim('depo: merkeze iplik girişi', async () => {
    await depo.sayfa.goto('/panel/merkez-stok');
    await depo.sayfa.getByRole('tab', { name: 'İplik girişi' }).click();
    await iplikSec(depo.sayfa, /İplik cinsi/, iplik);
    await depo.sayfa.getByLabel(/Gelen net kg/).fill('300');
    await depo.sayfa.getByRole('button', { name: 'İplik girişini kaydet' }).click();
    await depo.sayfa.getByText('İplik girişi kaydedildi.').waitFor();
  });

  const id = await taslakAc(mudur, iplik, '100');

  await depo.adim('ana sayfa: "Yüklenecek sevkiyat" iş listesi taslağı gösterir', async () => {
    await depo.sayfa.goto('/panel');
    const kart = depo.sayfa.getByTestId('pending-dispatches');
    await expect(kart).toBeVisible();
    await expect(kart).toContainText(/Yüklenecek sevkiyat: \d+/);
    await expect(kart).toContainText(/önce çuvalları ekleyin, sonra çıkış tartısını girin/i);
    depo.kontrol(
      'depo ana sayfasında "Karar bekleyen uyuşmazlıklar" listesi olmamalı (yalnızca karar yetkilisinde)',
      (await depo.sayfa.getByTestId('pending-discrepancies').count()) === 0,
    );
    depo.kontrol(
      'iş listesinde taslak sevkiyatın bağlantısı olmalı',
      (await kart.locator(`a[href$="${id}"]`).count()) === 1,
    );
  });

  await depo.adim('çuval yokken: tartı düğmesi görünmez, neden yazılı', async () => {
    await depo.sayfa.goto(`/panel/sevkiyat/${id}`);
    await expect(
      depo.sayfa.getByText(/Önce "Çuvallar" sekmesinden çuvalları ekleyin/),
    ).toBeVisible();
    await expect(depo.sayfa.getByRole('button', { name: 'Çıkış tartısını gir' })).toHaveCount(0);
    await expect(depo.sayfa.getByRole('link', { name: 'Çıkış tartısını gir' })).toHaveCount(0);
  });

  await cuvalEkle(depo, id, 2, '50');

  await depo.adim('çuval eklenince: uyarı kalkar, tartı bağlantısı açılır', async () => {
    await depo.sayfa.goto(`/panel/sevkiyat/${id}`);
    await expect(depo.sayfa.getByRole('link', { name: 'Çıkış tartısını gir' })).toBeVisible();
    depo.kontrol(
      'çuval varken "önce çuvalları ekleyin" uyarısı çıkmamalı',
      (await depo.sayfa.getByText(/Önce "Çuvallar" sekmesinden/).count()) === 0,
    );
  });

  await depo.adim(
    'tartı formu: önce boş araç (dara), sonra dolu araç (brüt); birim kg yazar',
    async () => {
      await depo.sayfa.goto(`/panel/sevkiyat/${id}/cikis`);
      const dara = await depo.sayfa.getByLabel(/Boş araç tartısı/).boundingBox();
      const brut = await depo.sayfa.getByLabel(/Dolu araç tartısı/).boundingBox();
      depo.kontrol(
        'dara alanı brüt alanının üstünde olmalı',
        dara !== null && brut !== null && dara.y < brut.y,
      );
      depo.kontrol(
        'kilo kutularında "kg" yazmalı',
        (await depo.sayfa.getByText('kg', { exact: true }).count()) >= 2,
      );
    },
  );

  const tam = await tamamlanmisSevkiyat({ depo, mudur }, iplik, '100');
  await kumasGir(fasonKalite, tam, '90', `${iplik}-K`);

  await mudur.adim('fire dosyası: düz cümle, harfsiz etiketler, hesap ayrıntıları', async () => {
    await mudur.sayfa.goto(`/panel/sevkiyat/${tam}`);
    await mudur.sayfa.getByRole('tab', { name: 'Fire dosyası', exact: true }).click();
    await mudur.sayfa.waitForTimeout(800);
    const metin = await mudur.sayfa.locator('main').innerText();
    mudur.kontrol(
      'özet cümlesi "Fasoncuya 100 kg iplik gönderildi, 90 kg kumaş geldi" olmalı',
      /Fasoncuya 100 kg iplik gönderildi, 90 kg kumaş geldi/.test(metin),
      metin.slice(0, 300),
    );
    mudur.kontrol('"Hesap ayrıntıları" bölümü olmalı', /Hesap ayrıntıları/.test(metin));
    mudur.kontrol(
      'harfli kısaltma ((G), (K), (T), (B), (I), (D)) kalmamalı',
      !/\((G|K|T|B|I|D)\)/.test(metin),
    );
    mudur.kontrol('"Hatalı kilo" yerine "Faturalanacak kilo"', !/Hatalı kilo/.test(metin));
  });

  await mudur.adim('bakiye: hareketlerde kod yok, iplik adı ve çalışan adı var', async () => {
    await mudur.sayfa.goto('/panel/bakiye');
    await mudur.sayfa.waitForLoadState('networkidle');
    const metin = await mudur.sayfa.locator('main').innerText();
    mudur.kontrol('bakiye ekranında UUID görünmemeli', !UUID.test(metin), metin.match(UUID)?.[0]);
    mudur.kontrol('iplik adı hareketlerde görünmeli', metin.includes(iplik));
    mudur.kontrol('"Başka bir kullanıcı" yazmamalı', !/Başka bir kullanıcı/.test(metin));
    // Tohum verisindeki eski satırın girenini sistem çözemez ("Bir çalışan" doğru yedek metindir);
    // bu koşuda açılan hareketlerde (robotun iplik adı geçen satırlar) kişi adı çözülmüş olmalı.
    const satirlar = await mudur.sayfa
      .locator('tbody tr')
      .filter({ hasText: iplik })
      .allInnerTexts();
    mudur.kontrol('bu koşunun hareket satırları bulundu', satirlar.length > 0);
    mudur.kontrol(
      'işlemi yapan kişinin adı yazmalı, "Bir çalışan" kalmamalı',
      satirlar.every((satir) => !/Bir çalışan/.test(satir)),
      satirlar.find((satir) => /Bir çalışan/.test(satir)),
    );
  });

  await mudur.adim('rapor: kumaşı gelmemiş sevkiyatta fire yerine "—"', async () => {
    await mudur.sayfa.goto('/panel/raporlar');
    await mudur.sayfa.getByRole('tab', { name: 'Fire raporu', exact: true }).click();
    await mudur.sayfa.waitForTimeout(900);
    const satirlar = await mudur.sayfa.locator('tbody tr').allInnerTexts();
    const bekleyen = satirlar.filter((s) => /Kumaş bekleniyor/.test(s));
    mudur.kontrol(
      'kumaş bekleyen satırlarda %100 fire yazmamalı',
      bekleyen.every((s) => !/%100/.test(s)),
      bekleyen.join(' | ').slice(0, 200),
    );
  });

  await kalite.adim(
    'tamamlanmış sevkiyatta kalitede "Fotoğraf ve belgeler / Yüklenecek belge ne?" alanı yok (itiraz sekmesinden girer)',
    async () => {
      await kalite.sayfa.goto(`/panel/sevkiyat/${tam}`);
      await kalite.sayfa.waitForLoadState('networkidle');
      const metin = await kalite.sayfa.locator('main').innerText();
      kalite.kontrol(
        'kalitede "Yüklenecek belge ne?" olmamalı',
        !/Yüklenecek belge ne\?/.test(metin),
      );
      await expect(kalite.sayfa.getByText('Fotoğraf ve belgeler', { exact: true })).toHaveCount(0);
      await expect(kalite.sayfa.getByRole('button', { name: 'Fotoğraf çek' })).toHaveCount(0);
    },
  );

  await mudur.adim(
    'tamamlanmış sevkiyatta müdürde kalite itirazı / laboratuvar belgesi yükleme alanı gelmez',
    async () => {
      await mudur.sayfa.goto(`/panel/sevkiyat/${tam}`);
      await mudur.sayfa.waitForLoadState('networkidle');
      await expect(mudur.sayfa.getByText('Yüklenecek belge ne?')).toHaveCount(0);
      await expect(mudur.sayfa.getByRole('button', { name: 'Fotoğraf çek' })).toHaveCount(0);
    },
  );
});
