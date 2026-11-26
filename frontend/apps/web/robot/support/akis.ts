import type { Page } from '@playwright/test';
import { expect } from './robot';
import type { Robot } from './robot';
import { iplikSec, resim, sec, tedarikciSec } from './yardimci';

/**
 * Müdür Tanımlar > İplikler'e yeni iplik ekler. Merkez stok girişi ve kumaş girişi yalnızca Tanımlar'daki aktif iplikle
 * yapılır; bu yüzden stokla çalışan her senaryo önce ipliği tanımlar. Kod benzersiz olmalıdır.
 */
export async function iplikTanimla(mudur: Robot, ad: string) {
  await mudur.sayfa.goto('/panel/tanimlar');
  await mudur.sayfa.getByRole('tab', { name: 'İplikler', exact: true }).click();
  await mudur.sayfa.getByRole('button', { name: 'Ekle' }).click();
  const pencere = mudur.sayfa.getByRole('dialog');
  // Aynı damgadan türeyen adlar (ör. `…-K`) aynı rakamları taşır; kod çakışmasın diye harf eki de eklenir.
  const ek = ad
    .replace(/^ROBOT/i, '')
    .replaceAll(/[^a-z]/gi, '')
    .slice(0, 6)
    .toUpperCase();
  await pencere.getByLabel('Kod *', { exact: true }).fill(`RB${ad.replaceAll(/\D/g, '')}${ek}`);
  await pencere.getByLabel('İplik adı *', { exact: true }).fill(ad);
  await pencere.getByRole('button', { name: 'Kaydet' }).click();
  await pencere.waitFor({ state: 'hidden' });
}

/**
 * Merkez stok > Kumaş girişi formunu doldurur (kaydetmez): cins, toplam kumaş kg, tek satırda iplik ve oranı, fire.
 * `iplik` boşsa iplik seçilmez ve oran yazılmaz.
 */
export async function merkezKumasDoldur(
  sayfa: Page,
  k: { kumas: string; iplik: string; kg: string; fire: string; oran?: string },
) {
  const cins = sayfa.getByRole('combobox', { name: /^Kumaş cinsi/ });
  if (k.kumas !== '' && (await cins.inputValue()) !== k.kumas) {
    await kumasCinsiSec(sayfa, k.kumas);
    // Cins seçilince satırlar tanımlı karışımla dolar; iplik ondan sonra değiştirilir.
    await expect(sayfa.getByLabel('Oran (%) 1')).toHaveValue('100');
  }
  await sayfa.getByLabel(/Toplam kumaş kg/).fill(k.kg);
  if (k.iplik !== '') {
    await iplikSec(sayfa, /İplik cinsi 1/, k.iplik);
    await sayfa.getByLabel('Oran (%) 1').fill(k.oran ?? '100');
  }
  await sayfa.getByLabel(/Fire \(%\)/).fill(k.fire);
}

/** Müdür Tanımlar > Kalite sınıfları'na yeni sınıf ekler (merkeze girişte yeni parti açarken seçilir). */
export async function kaliteSinifiTanimla(mudur: Robot, ad: string) {
  await mudur.sayfa.goto('/panel/tanimlar');
  await mudur.sayfa.getByRole('tab', { name: 'Kalite sınıfları', exact: true }).click();
  await mudur.sayfa.getByRole('button', { name: 'Ekle' }).click();
  const pencere = mudur.sayfa.getByRole('dialog');
  await pencere.getByLabel('Kod *', { exact: true }).fill(`RK${ad.replaceAll(/\D/g, '')}`);
  await pencere.getByLabel('Kalite adı *', { exact: true }).fill(ad);
  await pencere.getByRole('button', { name: 'Kaydet' }).click();
  await pencere.waitFor({ state: 'hidden' });
}

/**
 * Müdür Tanımlar > İplik tedarikçileri'ne yeni tedarikçi ekler. Merkeze iplik girişi yalnızca Tanımlar'daki aktif
 * tedarikçiyle yapılır; ad benzersiz olmalıdır.
 */
export async function tedarikciTanimla(mudur: Robot, ad: string) {
  await mudur.sayfa.goto('/panel/tanimlar');
  await mudur.sayfa.getByRole('tab', { name: 'İplik tedarikçileri', exact: true }).click();
  await mudur.sayfa.getByRole('button', { name: 'Ekle' }).click();
  const pencere = mudur.sayfa.getByRole('dialog');
  await pencere.getByLabel('Tedarikçi adı *', { exact: true }).fill(ad);
  await pencere.getByRole('button', { name: 'Kaydet' }).click();
  await pencere.waitFor({ state: 'hidden' });
}

/**
 * Müdür Tanımlar > Şoförler ve Araçlar'a bir merkez şoförü ile aracı ekler. Yeni sevkiyat formundaki "Merkez şoförleri"
 * ve araç listeleri yalnızca Tanımlar'daki kayıtlardan dolar; temiz ortamda bu kayıtlar yoktur. Plakayı döndürür.
 */
export async function soforAracTanimla(mudur: Robot, ad: string): Promise<string> {
  const sayi = ad.replaceAll(/\D/g, '');
  const plaka = `34 RBT ${sayi.slice(-3).padStart(3, '0')}`;
  for (const [sekme, alan, deger] of [
    ['Şoförler', 'Ad soyad *', ad],
    ['Araçlar', 'Plaka *', plaka],
  ] as const) {
    await mudur.sayfa.goto('/panel/tanimlar');
    await mudur.sayfa.getByRole('tab', { name: sekme, exact: true }).click();
    await mudur.sayfa.getByRole('button', { name: 'Ekle' }).click();
    const pencere = mudur.sayfa.getByRole('dialog');
    await pencere.getByLabel(alan, { exact: true }).fill(deger);
    await pencere.getByRole('button', { name: 'Kaydet' }).click();
    await pencere.waitFor({ state: 'hidden' });
  }
  return plaka;
}

/** Yeni sevkiyat formunda reçete satırının birim fiyatını (zorunlu, İplik sekmesinde) yazar. */
export async function fiyatYaz(sayfa: Page, fiyat = '21,5') {
  await sayfa
    .getByLabel(/Birim fiyat/)
    .first()
    .fill(fiyat);
}

/** Kumaş cinsini tanımlı karışımlar listesinden seçer. */
export async function kumasCinsiSec(sayfa: Page, ad: string) {
  await sayfa
    .getByRole('combobox', { name: /^Kumaş cinsi/ })
    .first()
    .click();
  await sayfa.getByRole('option', { name: ad, exact: true }).click();
}

/**
 * Sonradan değiştirilemeyen kayıtlar iki adımlıdır: kaydet düğmesine basar, onay sorulduysa "Evet, kaydet" der.
 * Eksik ya da hatalı formda onay sorulmaz.
 */
export async function kaydetOnayla(sayfa: Page, dugme: string) {
  await sayfa.getByRole('button', { name: dugme }).click();
  await sayfa
    .getByRole('button', { name: 'Evet, kaydet' })
    .click({ timeout: 2_000 })
    .catch(() => undefined);
}

/**
 * Kumaş cinsi yalnızca tanımlı karışımlardan seçilir; senaryoların kullandığı "Süprem" karışımı yoksa açılır. İpliği
 * hiçbir stokta olmayan ayrı bir ipliktir: fasoncu kumaş girişinde oranlar boş gelir, senaryo kendisi yazar.
 */
export async function supremKarisimiTanimla(mudur: Robot) {
  const iplik = 'Robot Süprem ipliği';
  const karisimlar = await mudur.api<{ fabricType: string }[]>('/api/v1/fabric-recipes');
  if (karisimlar.some((k) => k.fabricType === 'Süprem')) return;
  await iplikTanimla(mudur, iplik);
  await mudur.sayfa.goto('/panel/tanimlar');
  await mudur.sayfa.getByRole('tab', { name: 'Kumaş karışımı', exact: true }).click();
  await mudur.sayfa.getByRole('button', { name: 'Karışım ekle' }).click();
  await mudur.sayfa.getByLabel(/Kumaş cinsi/).fill('Süprem');
  await mudur.sayfa.getByRole('combobox', { name: 'İplik', exact: true }).click();
  await mudur.sayfa.getByRole('option', { name: iplik, exact: true }).click();
  await mudur.sayfa.getByLabel('Oran (%)').fill('100');
  await mudur.sayfa.getByRole('button', { name: 'Kaydet' }).click();
  await expect(mudur.sayfa.getByText('Süprem').first()).toBeVisible();
}

/** Yeni sevkiyat formunda ilk satırın ipliğini listeden seçer; iplik Tanımlar'da olmalıdır (serbest metin yoktur). */
export async function sevkiyatIpligiSec(sayfa: Page, iplik: string) {
  const liste = sayfa.getByRole('combobox', { name: 'İplik cinsi' }).first();
  await liste.click();
  await liste.fill(iplik);
  await sayfa
    .locator('[role=option]:visible')
    .filter({ hasText: iplik })
    .first()
    .click({ timeout: 15_000 });
}

export type Yonetim = { depo: Robot; mudur: Robot };

/** Kapıdan aktarmada `tedarikci`, Tanımlar > İplik tedarikçileri'nde tanımlı olmalıdır. */
export type Kaynak =
  { tur: 'merkez' } | { tur: 'fason'; gonderen: string } | { tur: 'kapi'; tedarikci: string };

export async function taslakAc(
  mudur: Robot,
  iplik: string,
  kg: string,
  fire = '3',
  kaynak: Kaynak = { tur: 'merkez' },
  alici = 'Başka Dokuma',
  tarih?: string,
): Promise<string> {
  await mudur.sayfa.goto('/panel/sevkiyat/yeni');
  await sec(mudur.sayfa, /Alıcı fasoncu/, alici);
  if (kaynak.tur === 'fason') {
    await mudur.sayfa.locator('label').filter({ hasText: 'Başka fasoncu' }).first().click();
    await sec(mudur.sayfa, /Gönderen fasoncu/, kaynak.gonderen);
  } else if (kaynak.tur === 'kapi') {
    await mudur.sayfa.locator('label').filter({ hasText: 'Kapıdan aktarma' }).first().click();
    // Tedarikçi serbest metin değil; Tanımlar'daki aktif kayıttan seçilir (önce tedarikciTanimla).
    await tedarikciSec(mudur.sayfa, kaynak.tedarikci);
  }
  await mudur.sayfa.getByLabel(/Fire oranı/).fill(fire);
  // Sevkiyat tarihi formda bugünle gelir; verilirse o güne ayarlanır.
  if (tarih !== undefined) await mudur.sayfa.getByLabel(/Sevkiyat tarihi/).fill(tarih);
  // İplik yalnızca Tanımlar'daki ipliklerden seçilir (serbest metin alanı yoktur); listede yoksa önce tanımlanır ve form
  // baştan doldurulur.
  const liste = mudur.sayfa.getByRole('combobox', { name: 'İplik cinsi' }).first();
  await liste.click();
  await liste.fill(iplik);
  const secenek = mudur.sayfa.locator('[role=option]:visible').filter({ hasText: iplik }).first();
  if (
    await secenek.waitFor({ timeout: 3_000 }).then(
      () => true,
      () => false,
    )
  ) {
    await secenek.click();
  } else {
    await iplikTanimla(mudur, iplik);
    return taslakAc(mudur, iplik, kg, fire, kaynak, alici, tarih);
  }
  await mudur.sayfa.getByLabel(/Net kg 1/).fill(kg);
  await fiyatYaz(mudur.sayfa);
  await mudur.sayfa.getByRole('button', { name: 'Taslak oluştur' }).click();
  await mudur.sayfa.waitForURL(/sevkiyat\/[0-9a-f-]{30,}$/);
  return mudur.sayfa.url().split('/').pop() ?? '';
}

/** Depo her biri `kg` kilo olan `adet` çuval ekler. */
export async function cuvalEkle(depo: Robot, id: string, adet: number, kg: string) {
  await depo.sayfa.goto(`/panel/sevkiyat/${id}`);
  // Adım çubuğunda da "Çuvallar" yazar; sekme rolüyle seçilir.
  await depo.sayfa.getByRole('tab', { name: 'Çuvallar', exact: true }).click();
  for (let i = 0; i < adet; i += 1) {
    await depo.sayfa.getByLabel(/Çuval kilosu/).fill(kg);
    await depo.sayfa.getByRole('button', { name: 'Listeye ekle' }).click();
  }
  await depo.sayfa.getByRole('button', { name: /çuvalı ekle/ }).click();
  await depo.sayfa.getByText(`Sevkiyattaki çuvallar (${String(adet)})`).waitFor();
}

/**
 * Depo "İleri"ye bastıktan sonra açılan onay penceresinde "Onayla…" der. Pencere açılmazsa (alan uyarısı çıktıysa,
 * ör. eksik kilo nedeni boş) bir şey yapmaz; uyarıyı çağıran senaryo denetler.
 */
export async function tartiyiOnayla(depo: Robot) {
  const onay = depo.sayfa.getByRole('dialog').getByRole('button', { name: /^Onayla/ });
  const acildi = await onay
    .waitFor({ state: 'visible', timeout: 2000 })
    .then(() => true)
    .catch(() => false);
  if (acildi) await onay.click();
}

/** Depo çıkış tartısını girer (kantar fişi fotoğrafıyla); "İleri" ve onay adımını da yapar. */
export async function tartiGir(depo: Robot, id: string, brut: string, dara: string, cuval: string) {
  await depo.sayfa.goto(`/panel/sevkiyat/${id}/cikis`);
  await depo.sayfa.getByLabel(/Dolu araç tartısı/).fill(brut);
  await depo.sayfa.getByLabel(/Boş araç tartısı/).fill(dara);
  await depo.sayfa.getByLabel(/Çuval sayısı/).fill(cuval);
  await depo.sayfa
    .locator('input[type=file]')
    .first()
    .setInputFiles({ name: 'fis.jpg', mimeType: 'image/jpeg', buffer: await resim(depo.sayfa, 3) });
  await depo.sayfa
    .getByText(/Fotoğraf seçildi/)
    .first()
    .waitFor();
  await depo.sayfa.getByRole('button', { name: 'İleri' }).click();
  await tartiyiOnayla(depo);
}

/** Müdür irsaliye yükleyip sevkiyatı tamamlar. */
export async function tamamla(mudur: Robot, id: string, irsaliyeNo: string) {
  await mudur.sayfa.goto(`/panel/sevkiyat/${id}`);
  await mudur.sayfa
    .locator('input[type=file]')
    .first()
    .setInputFiles({
      name: 'irs.jpg',
      mimeType: 'image/jpeg',
      buffer: await resim(mudur.sayfa, 5),
    });
  await mudur.sayfa.getByText('İrsaliye yüklendi').waitFor();
  await mudur.sayfa.getByLabel(/İrsaliye no/).fill(irsaliyeNo);
  await mudur.sayfa.getByRole('button', { name: 'Sevkiyatı tamamla' }).click();
  await expect(mudur.sayfa.locator('main')).toContainText(/tamamlandı/i);
}

/** Merkez stok yetmeyen sevkiyatı tamamlar: uyarı çıkmadan hemen tamamlanır (eksiği sistem girer). */
export async function stokYetmeseDeTamamla(mudur: Robot, id: string, irsaliyeNo: string) {
  await mudur.sayfa.goto(`/panel/sevkiyat/${id}`);
  await mudur.sayfa
    .locator('input[type=file]')
    .first()
    .setInputFiles({
      name: 'irs.jpg',
      mimeType: 'image/jpeg',
      buffer: await resim(mudur.sayfa, 5),
    });
  await mudur.sayfa.getByText('İrsaliye yüklendi').waitFor();
  await mudur.sayfa.getByLabel(/İrsaliye no/).fill(irsaliyeNo);
  await mudur.sayfa.getByRole('button', { name: 'Sevkiyatı tamamla' }).click();
  await expect(mudur.sayfa.locator('main')).toContainText(/tamamlandı/i);
}

/** Taslaktan tamamlanmışa tüm yol: `kg` kilo iplik, tek çuval grubu. Sevkiyat kimliğini döner. */
export async function tamamlanmisSevkiyat(
  y: Yonetim,
  iplik: string,
  kg = '100',
  fire = '3',
  kaynak: Kaynak = { tur: 'merkez' },
  alici = 'Başka Dokuma',
): Promise<string> {
  const yarim = String(Number(kg) / 2);
  const id = await taslakAc(y.mudur, iplik, kg, fire, kaynak, alici);
  await cuvalEkle(y.depo, id, 2, yarim);
  await tartiGir(y.depo, id, String(Number(kg) + 3.5).replace('.', ','), '3,5', '2');
  await y.depo.sayfa.waitForURL(new RegExp(`${id}$`), { timeout: 15_000 });
  await tamamla(y.mudur, id, `${iplik}-IRS`);
  return id;
}

/** Fasoncu kalite kumaş formunu açıp doldurur (iplik oranı yazmaz, `oranYaz` ayrı); kaydetmez. */
export async function kumasFormuDoldur(
  fasonKalite: Robot,
  id: string,
  kg: string,
  irsaliyeNo: string,
) {
  await fasonKalite.sayfa.goto(`/panel/sevkiyat/${id}`);
  await fasonKalite.sayfa.getByRole('tab', { name: 'Kumaş girişi', exact: true }).click();
  await fasonKalite.sayfa.waitForTimeout(600);
  await kumasCinsiSec(fasonKalite.sayfa, 'Süprem');
  await fasonKalite.sayfa.getByLabel(/Kumaş net kilosu/).fill(kg);
  await fasonKalite.sayfa.getByLabel(/İrsaliye no/).fill(irsaliyeNo);
}

/** Metni düzenli ifade içinde olduğu gibi aratır. */
const kacisli = (metin: string) => metin.replace(/[/\\^$*+?.()|[\]{}]/g, '\\$&');

/**
 * Kumaşın iplik karışımını yazar: `oranlar` iplik adı → yüzde (ör. `{ [iplik]: '60', [baskaIplik]: '40' }`); verilmezse
 * bu sevkiyatın ipliğine %100 yazılır. Oran kutusunun etiketi "Oran: <iplik adı>"dır (partisi varsa adın yanında).
 */
export async function oranYaz(fasonKalite: Robot, oranlar?: Record<string, string>) {
  const sayfa = fasonKalite.sayfa;
  if (oranlar === undefined) {
    // Listede fasoncuda başka sevkiyatlardan kalan iplikler de durur; oran bu sevkiyatın ipliğine yazılır.
    const id = /\/sevkiyat\/([^/?]+)/.exec(sayfa.url())?.[1] ?? '';
    const secenekler = await fasonKalite.api<{ yarnType: string; ownKg: string }[]>(
      `/api/v1/shipments/${id}/fabric-receipts/yarn-options`,
    );
    const kendi = secenekler.find((s) => Number(s.ownKg) > 0)?.yarnType;
    const kutu =
      kendi === undefined
        ? sayfa.getByLabel(/^Oran: /)
        : sayfa.getByLabel(new RegExp(`^Oran: ${kacisli(kendi)}(?![-\\w])`));
    await kutu.first().fill('100');
    return;
  }
  for (const [iplik, oran] of Object.entries(oranlar)) {
    await sayfa.getByLabel(new RegExp(`^Oran: ${kacisli(iplik)}`)).fill(oran);
  }
}

/**
 * Fasoncu kalite, fasoncudan gelen kumaşı girer (müdür kumaş giremez). Önce iplik oranları yazılır, "Kontrol et"
 * hangi sevkiyattan kaç kilo düşeceğini gösterir; kayıt sonradan değiştirilemediği için "Evet, kaydet" sorulur.
 * `onayla` false ise kontrol yapılır ama onay verilmez (ör. fasoncuda iplik yetmiyorsa onay sorusu hiç çıkmaz).
 */
export async function kumasGir(
  fasonKalite: Robot,
  id: string,
  kg: string,
  irsaliyeNo: string,
  onayla = true,
  oranlar?: Record<string, string>,
) {
  await kumasFormuDoldur(fasonKalite, id, kg, irsaliyeNo);
  await oranYaz(fasonKalite, oranlar);
  await fasonKalite.sayfa.getByRole('button', { name: 'Kontrol et' }).click();
  if (!onayla) {
    await fasonKalite.sayfa.waitForTimeout(1000);
    return;
  }
  await fasonKalite.sayfa.getByRole('button', { name: 'Evet, kaydet' }).click({ timeout: 15_000 });
  await fasonKalite.sayfa.getByText('Kumaş girişi kaydedildi.').waitFor({ timeout: 15_000 });
}

/**
 * Kalite, açık uyuşmazlıkta karar verir. Karar geri alınamadığı için "Karara bağla" önce "Karar geri alınamaz."
 * diye sorar; `onayla` false ise onay verilmez.
 */
export async function kararVer(kalite: Robot, karar: string, not: string, onayla = true) {
  await sec(kalite.sayfa, /^Karar/, karar);
  await kalite.sayfa
    .getByLabel(/^Not(?! \()/)
    .first()
    .fill(not);
  await kalite.sayfa.getByRole('button', { name: 'Karara bağla' }).click();
  await kalite.sayfa.getByText(/Karar geri alınamaz\./).waitFor({ timeout: 15_000 });
  if (onayla) {
    await kalite.sayfa.getByRole('button', { name: 'Evet, kararı kaydet' }).click();
  }
}
