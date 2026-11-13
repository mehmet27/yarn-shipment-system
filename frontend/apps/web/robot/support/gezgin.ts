import type { Locator, Page } from '@playwright/test';
import type { Robot } from './robot';

/** Veri değiştirmeyen, bu yüzden gezginin kendi tıkladığı düğmeler (sayfalama, indirme, büyütme, satır ekle/sil…). */
const GUVENLI_DUGME =
  /^(\d+|Önceki sayfa|Sonraki sayfa|PDF indir|Excel indir \(\.xlsx\)|Sipariş formu PDF|.*fotoğrafını büyüt|Satır ekle|İplik ekle|İplik satırı ekle|Sil|\d+\. satırı sil|Varsayılana dön|Yeni kullanıcı|Ekle|Bildirimler|Yan menü|Yenile)$/;

/** Dosya seçtirdiği ya da kamera açtığı için gezginin dokunmadığı düğmeler; dosya yükleme senaryolarında denenir. */
const DOSYA_DUGME =
  /^(Fotoğraf çek|Dosya seç|Logo seç|Rapor logosu ekle|Yazdır|Yeniden çek|Fotoğrafı gönder|Ekrandan renk seç)$/;

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
    await sayfa.goto(yol);
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
          robot.kontrol(
            `'${oge.ad}' seçilince işaretli olmalı`,
            await girdi
              .first()
              .isChecked()
              .catch(() => true),
          );
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
