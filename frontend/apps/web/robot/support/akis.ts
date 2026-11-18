import type { Page } from '@playwright/test';
import { expect } from './robot';
import type { Robot } from './robot';
import { resim, sec } from './yardimci';

/**
 * Müdür Tanımlar > İplikler'e yeni iplik ekler. Merkez stok girişi ve kumaş girişi yalnızca Tanımlar'daki aktif iplikle
 * yapılır; bu yüzden stokla çalışan her senaryo önce ipliği tanımlar. Kod benzersiz olmalıdır.
 */
export async function iplikTanimla(mudur: Robot, ad: string) {
  await mudur.sayfa.goto('/panel/tanimlar');
  await mudur.sayfa.getByRole('tab', { name: 'İplikler', exact: true }).click();
  await mudur.sayfa.getByRole('button', { name: 'Ekle' }).click();
  const pencere = mudur.sayfa.getByRole('dialog');
  await pencere.getByLabel('Kod *', { exact: true }).fill(`RB${ad.replaceAll(/\D/g, '')}`);
  await pencere.getByLabel('İplik adı *', { exact: true }).fill(ad);
  await pencere.getByRole('button', { name: 'Kaydet' }).click();
  await pencere.waitFor({ state: 'hidden' });
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

/** Yeni sevkiyat formunda reçete satırının birim fiyatını (zorunlu) "Sipariş bilgileri" sekmesinden yazar. */
export async function fiyatYaz(sayfa: Page, fiyat = '21,5') {
  await sayfa
    .getByRole('tab', { name: /Sipariş bilgileri/ })
    .first()
    .click();
  await sayfa
    .getByLabel(/Birim fiyat/)
    .first()
    .fill(fiyat);
  // Sekme seçimi formda kalır; sonraki adımlar iplik/kilo alanlarını arayacağı için İplik sekmesine dön.
  await sayfa.getByRole('tab', { name: 'İplik', exact: true }).first().click();
}

export type Yonetim = { depo: Robot; mudur: Robot };

/** Müdür taslağı açar; sevkiyat kimliğini döner. */
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
    await mudur.sayfa.getByLabel(/^Tedarikçi/).fill(kaynak.tedarikci);
  }
  await mudur.sayfa.getByLabel(/Fire oranı/).fill(fire);
  // Sevkiyat tarihi formda bugünle gelir; verilirse o güne ayarlanır.
  if (tarih !== undefined) await mudur.sayfa.getByLabel(/Sevkiyat tarihi/).fill(tarih);
  await mudur.sayfa
    .getByLabel(/ya da listede yoksa yazın/i)
    .first()
    .fill(iplik);
  await mudur.sayfa.getByLabel(/Net kg 1/).fill(kg);
  await fiyatYaz(mudur.sayfa);
  await mudur.sayfa.getByRole('button', { name: 'Taslak oluştur' }).click();
  await mudur.sayfa.waitForURL(/sevkiyat\/[0-9a-f-]{30,}$/);
  return mudur.sayfa.url().split('/').pop() ?? '';
}

/** Depo her biri `kg` kilo olan `adet` çuval ekler. */
export async function cuvalEkle(depo: Robot, id: string, adet: number, kg: string) {
  await depo.sayfa.goto(`/panel/sevkiyat/${id}`);
  await depo.sayfa.getByText('Çuvallar', { exact: true }).first().click();
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

/** Fasoncu kalite kumaş formunu açıp doldurur (iplik satırı eklemez); kaydetmez. */
export async function kumasFormuDoldur(
  fasonKalite: Robot,
  id: string,
  kg: string,
  irsaliyeNo: string,
) {
  await fasonKalite.sayfa.goto(`/panel/sevkiyat/${id}`);
  await fasonKalite.sayfa.getByRole('tab', { name: 'Kumaş girişi', exact: true }).click();
  await fasonKalite.sayfa.waitForTimeout(600);
  await fasonKalite.sayfa.getByLabel(/Kumaş cinsi/).fill('Süprem');
  await fasonKalite.sayfa.getByLabel(/Kumaş net kilosu/).fill(kg);
  await fasonKalite.sayfa.getByLabel(/İrsaliye no/).fill(irsaliyeNo);
}

/**
 * Fasoncu kalite, fasoncudan gelen kumaşı girer (müdür kumaş giremez). Kayıt sonradan değiştirilemediği için
 * "Kumaş girişini kaydet" önce özet ve "Evet, kaydet" sorar; `onayla` false ise onay verilmez (ör. kilo kalanı aşıyorsa
 * onay sorusu hiç çıkmaz).
 */
export async function kumasGir(
  fasonKalite: Robot,
  id: string,
  kg: string,
  irsaliyeNo: string,
  onayla = true,
) {
  await kumasFormuDoldur(fasonKalite, id, kg, irsaliyeNo);
  await fasonKalite.sayfa.getByRole('button', { name: 'Kumaş girişini kaydet' }).click();
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
