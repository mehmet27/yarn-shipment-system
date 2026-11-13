import { expect } from './robot';
import type { Robot } from './robot';
import { resim, sec } from './yardimci';

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
  await mudur.sayfa
    .getByLabel(/serbest metin/i)
    .first()
    .fill(iplik);
  await mudur.sayfa.getByLabel(/Net kg 1/).fill(kg);
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

/** Depo çıkış tartısını girer (kantar fişi fotoğrafıyla). */
export async function tartiGir(depo: Robot, id: string, brut: string, dara: string, cuval: string) {
  await depo.sayfa.goto(`/panel/sevkiyat/${id}/cikis`);
  await depo.sayfa.getByLabel(/Dolu araç tartısı/).fill(brut);
  await depo.sayfa.getByLabel(/Boş araç tartısı/).fill(dara);
  await depo.sayfa.getByLabel(/Çuval sayısı/).fill(cuval);
  await depo.sayfa
    .locator('input[type=file]')
    .first()
    .setInputFiles({ name: 'fis.jpg', mimeType: 'image/jpeg', buffer: await resim(depo.sayfa, 3) });
  await depo.sayfa.getByRole('button', { name: 'Fotoğrafı gönder' }).click();
  await depo.sayfa
    .getByText(/fotoğrafı kaydedildi/)
    .first()
    .waitFor();
  await depo.sayfa.getByRole('button', { name: 'Kaydet' }).click();
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
