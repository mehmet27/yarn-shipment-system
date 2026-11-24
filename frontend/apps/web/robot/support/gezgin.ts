import type { Locator, Page } from '@playwright/test';
import type { Robot } from './robot';

/**
 * Veri değiştirmeyen, bu yüzden gezginin kendi tıkladığı düğmeler (sayfalama, indirme, büyütme, satır ekle/sil,
 * "sıradaki adım" kutusunun sekme açan ya da forma kaydıran düğmeleri…).
 */
const GUVENLI_DUGME =
  /^(\d+|Önceki sayfa|Sonraki sayfa|PDF indir|Excel indir \(\.xlsx\)|Sipariş formu PDF|.*fotoğrafını büyüt|Satır ekle|İplik ekle|İplik satırı ekle|Sil|\d+\. satırı sil|Varsayılana dön|Yeni kullanıcı|Ekle|Bildirimler|Yan menü|Yenile|Çuval eklemeye başla|İrsaliyeyi yükle|Karar seçeneklerine git|Kumaş girişi yap|Fire dosyasını aç|Tüm adımlar|Adımları gizle|Tüm durumları göster|Süzgeçleri temizle|Ara)$/;

/** Dosya seçtirdiği ya da kamera açtığı için gezginin dokunmadığı düğmeler; dosya yükleme senaryolarında denenir. */
const DOSYA_DUGME =
  /^(Fotoğraf çek|Dosya seç|Logo seç|Rapor logosu ekle|Yazdır|Yeniden çek|Ekrandan renk seç)$/;

/** Kişiye özgü düğme adlarını ortak ada çevirir (ör. 'depo hesabını düzenle' → 'Hesabı düzenle'). */
const genelAd = (ad: string) =>
  ad
    .replace(/^\S+ hesabını düzenle$/, 'Hesabı düzenle')
    .replace(/^\S+ için diğer işlemler$/, 'Diğer işlemler')
    .replace(/^\d+\. satırı sil$/, 'Satırı sil')
    .replace(/^\d+ satırı yükle$/, 'Satırları yükle')
    .replace(/^\d+ çuvalı ekle$/, 'Çuvalları ekle');

export type Kapsam = {
  rota: string;
  ad: string;
  durum: 'tıklandı' | 'senaryoda' | 'AÇIKTA' | 'dosya';
};

/** Rota adındaki kimlikleri kısaltır: /panel/sevkiyat/<id> */
export const rotaTipi = (yol: string) => yol.replace(/\/[0-9a-f-]{30,}/g, '/<id>');

async function hataVar(sayfa: Page): Promise<string | undefined> {
  const metin = await sayfa.locator('body').innerText();
  return /Beklenmeyen bir hata|Aradığınız sayfa bulunamadı/.test(metin)
    ? 'hata sayfası göründü'
    : undefined;
}

/**
 * Bir sayfadaki her sekme, filtre, anahtar, liste ve güvenli düğmeye tıklar. Veri değiştiren düğmeler tıklanmaz; adı
 * `kapsanan` kayıtlarında (senaryolarda denendiği belirtilenler) yoksa "AÇIKTA" olarak raporlanır.
 */
export async function sayfayiGez(
  robot: Robot,
  yol: string,
  kapsanan: ReadonlySet<string>,
): Promise<Kapsam[]> {
  const { sayfa } = robot;
  const sonuc: Kapsam[] = [];
  const rota = rotaTipi(yol);
  const ac = async () => {
    // Gezgin tek sekmede yüzlerce kez sayfa açar; veri çoğaldıkça (tam koşu) tarayıcı kaynak sınırına takılıp
    // ERR_INSUFFICIENT_RESOURCES verebilir. Sekme boşaltılıp kısa bir aradan sonra yeniden denenir.
    for (let deneme = 0; ; deneme += 1) {
      try {
        await sayfa.goto(yol);
        break;
      } catch (hata) {
        if (deneme >= 2 || !/ERR_INSUFFICIENT_RESOURCES/.test((hata as Error).message)) throw hata;
        await sayfa.goto('about:blank').catch(() => undefined);
        await sayfa.waitForTimeout(2_000 * (deneme + 1));
      }
    }
    await sayfa.waitForLoadState('networkidle');
    await sayfa.waitForTimeout(300);
  };
  await ac();
  const ogeler = await sayfa
    .locator(
      'main button, main [role=tab], main input[type=radio], main input[type=checkbox], main [role=combobox]',
    )
    .evaluateAll((liste) =>
      liste.map((e) => {
        const etiket = e.id === '' ? null : document.querySelector(`label[for="${e.id}"]`);
        const ad = (etiket?.textContent ?? e.getAttribute('aria-label') ?? e.textContent ?? '')
          .trim()
          .replace(/\s+/g, ' ');
        const tur =
          e.getAttribute('role') === 'tab'
            ? 'sekme'
            : e.getAttribute('role') === 'combobox'
              ? 'liste'
              : (e as HTMLInputElement).type === 'radio'
                ? 'radyo'
                : (e as HTMLInputElement).type === 'checkbox'
                  ? 'anahtar'
                  : 'düğme';
        return { ad: ad.slice(0, 60), tur, pasif: (e as HTMLButtonElement).disabled };
      }),
    );
  const gorulen = new Set<string>();
  /** Sekme açılınca beliren düğmeleri sınıflar: güvenli olanlar sayılır, değiştirenler kapsam kaydına bakılır. */
  const sekmeIcini = async (sekme: string) => {
    const adlar = await sayfa
      .locator('main button:not([role=tab])')
      .evaluateAll((liste) =>
        liste.map((e) => (e.textContent ?? '').trim().replace(/\s+/g, ' ').slice(0, 60)),
      );
    for (const ham of adlar) {
      const ad = genelAd(ham);
      const anahtar = `düğme|${ad}`;
      if (ad === '' || gorulen.has(anahtar)) continue;
      gorulen.add(anahtar);
      const etiket = `düğme (${sekme} sekmesi): ${ad}`;
      if (DOSYA_DUGME.test(ad)) sonuc.push({ rota, ad: etiket, durum: 'dosya' });
      else if (GUVENLI_DUGME.test(ad)) sonuc.push({ rota, ad: etiket, durum: 'tıklandı' });
      else
        sonuc.push({
          rota,
          ad: etiket,
          durum: kapsanan.has(`${rota}|${ad}`) || kapsanan.has(`*|${ad}`) ? 'senaryoda' : 'AÇIKTA',
        });
    }
  };
  for (const oge of ogeler) {
    oge.ad = genelAd(oge.ad);
    const anahtar = `${oge.tur}|${oge.ad}`;
    if (oge.ad === '' || gorulen.has(anahtar)) continue;
    gorulen.add(anahtar);
    const kayit = (durum: Kapsam['durum']) =>
      sonuc.push({ rota, ad: `${oge.tur}: ${oge.ad}`, durum });
    if (oge.tur === 'düğme' && DOSYA_DUGME.test(oge.ad)) {
      kayit('dosya');
      continue;
    }
    if (oge.tur === 'düğme' && !GUVENLI_DUGME.test(oge.ad)) {
      kayit(
        kapsanan.has(`${rota}|${oge.ad}`) || kapsanan.has(`*|${oge.ad}`) ? 'senaryoda' : 'AÇIKTA',
      );
      continue;
    }
    if (oge.pasif) continue;
    await robot.adim(`${rota} › ${oge.tur} '${oge.ad}' tıkla`, async () => {
      const hedef: Locator =
        oge.tur === 'sekme'
          ? sayfa.getByRole('tab', { name: oge.ad, exact: true })
          : oge.tur === 'liste'
            ? sayfa.getByLabel(oge.ad, { exact: true })
            : oge.tur === 'radyo'
              ? sayfa.locator('label').filter({ hasText: oge.ad })
              : oge.tur === 'anahtar'
                ? sayfa.getByLabel(oge.ad, { exact: true })
                : sayfa.getByRole('button', { name: oge.ad, exact: true });
      const ilk = hedef.first();
      // Başka bir sekmenin arkasında kalan alanlar o sekme açılınca tıklanır.
      if (oge.tur === 'liste' && !(await ilk.isVisible())) return;
      // Radyo ve anahtar başka bir sekmenin arkasında kalmış olabilir (ör. Merkez iplik stoku "Ada göre"):
      // görünene kadar sekmeleri açarak alanı ortaya çıkarır; hiçbir sekmede görünmüyorsa tıklamayı atlar.
      if ((oge.tur === 'radyo' || oge.tur === 'anahtar') && !(await ilk.isVisible())) {
        const sekmeler = sayfa.getByRole('tab');
        for (let i = 0; i < (await sekmeler.count()); i++) {
          await sekmeler.nth(i).click();
          await sayfa.waitForTimeout(250);
          if (await ilk.isVisible()) break;
        }
        if (!(await ilk.isVisible())) return;
      }
      if (/PDF indir|Excel indir|Sipariş formu PDF/.test(oge.ad)) {
        const [indirme] = await Promise.all([
          sayfa.waitForEvent('download', { timeout: 15_000 }),
          ilk.click(),
        ]);
        robot.kontrol(`${oge.ad}: dosya inmeli`, indirme.suggestedFilename() !== '');
      } else if (oge.tur === 'liste') {
        const once = `${sayfa.url()}|${(await sayfa.locator('main').innerText()).length}`;
        await ilk.click();
        await sayfa.waitForTimeout(300);
        const secenekler = sayfa.locator('[role=option]:visible');
        const adet = await secenekler.count();
        robot.kontrol(`'${oge.ad}' listesinde seçenek olmalı`, adet > 0);
        // Süzgeç listelerinde ikinci seçeneği seçip sonucun (adres ya da liste) gerçekten değiştiğini doğrular.
        if (adet > 1 && /^(Durum|Önem|Kayıt türü|Fasoncu|İplik cinsi)$/.test(oge.ad)) {
          await secenekler.nth(1).click();
          await sayfa.waitForLoadState('networkidle');
          await sayfa.waitForTimeout(400);
          const sonra = `${sayfa.url()}|${(await sayfa.locator('main').innerText()).length}`;
          robot.kontrol(`'${oge.ad}' süzgeci seçilince sonuç değişmeli`, sonra !== once, once);
        } else {
          await sayfa.keyboard.press('Escape');
        }
      } else {
        const oncekiMetin = await sayfa.locator('main').innerText();
        // Anahtar zaten açıksa tıklayınca kapanır (ör. Yetkiler ekranında rolün varsayılan izinleri):
        // beklenen sonuç "işaretli" değil, "tıklamadan önceki durumun tersi"dir.
        const oncekiIsaret =
          oge.tur === 'anahtar' ? await ilk.isChecked().catch(() => undefined) : undefined;
        await ilk.click();
        await sayfa.waitForLoadState('networkidle');
        await sayfa.waitForTimeout(250);
        if (oge.tur === 'sekme') {
          robot.kontrol(
            `'${oge.ad}' sekmesi seçili olmalı`,
            (await hedef.first().getAttribute('aria-selected')) === 'true',
          );
        } else if (oge.tur === 'radyo' || oge.tur === 'anahtar') {
          const girdi =
            oge.tur === 'radyo'
              ? sayfa.getByRole('radio', { name: oge.ad, exact: true })
              : sayfa.getByLabel(oge.ad, { exact: true });
          const simdi = await girdi
            .first()
            .isChecked()
            .catch(() => undefined);
          if (oge.tur === 'anahtar' && oncekiIsaret !== undefined) {
            robot.kontrol(
              `'${oge.ad}' tıklanınca durumu değişmeli (${oncekiIsaret ? 'açıktı, kapanmalı' : 'kapalıydı, açılmalı'})`,
              simdi === !oncekiIsaret,
            );
          } else {
            robot.kontrol(`'${oge.ad}' seçilince işaretli olmalı`, simdi ?? true);
          }
        } else if (/sayfa$/.test(oge.ad)) {
          robot.kontrol(
            `'${oge.ad}' liste içeriğini değiştirmeli`,
            (await sayfa.locator('main').innerText()) !== oncekiMetin,
          );
        }
        if (/büyüt/.test(oge.ad)) await sayfa.keyboard.press('Escape');
        const hata = await hataVar(sayfa);
        robot.kontrol(`${rota} › ${oge.ad}: sayfa hatasız kalmalı`, hata === undefined, hata);
      }
      kayit('tıklandı');
      if (oge.tur === 'sekme') await sekmeIcini(oge.ad);
    });
    // Her tıklamadan sonra sayfa temiz durumuna döner (açılan pencere, süzgeç, sayfa numarası sıfırlanır).
    if (/büyüt|Yeni kullanıcı|Ekle$/.test(oge.ad) || oge.tur !== 'sekme') await ac();
  }
  return sonuc;
}
