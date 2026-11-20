import { cuvalEkle, iplikTanimla, kumasGir, tamamla, tartiGir, taslakAc } from '../support/akis';
import { expect, test } from '../support/robot';
import type { Robot } from '../support/robot';
import { damga } from '../support/yardimci';

/** Sevkiyat dosyasındaki "Sıradaki adım" kutusunun metni. */
async function siradakiAdim(robot: Robot, id: string): Promise<string> {
  await robot.sayfa.goto(`/panel/sevkiyat/${id}`);
  const kutu = robot.sayfa.getByTestId('next-step');
  await kutu.waitFor();
  return kutu.innerText();
}

/**
 * Ana sayfadaki iş listesi kartı görünüyor ve başlığında en az bir iş sayıyor mu. Kart en çok 5-20 satır gösterdiği için
 * (dolu ortamda bu sevkiyat listeye sığmayabilir) satır değil, kartın kendisi ve sayısı denetlenir.
 */
async function anaSayfadaVar(robot: Robot, kart: string): Promise<boolean> {
  await robot.sayfa.goto('/panel');
  await robot.sayfa.getByTestId('home-tasks').waitFor();
  await robot.sayfa.waitForLoadState('networkidle');
  const kutu = robot.sayfa.getByTestId(kart);
  if ((await kutu.count()) === 0) return false;
  return /: [1-9]\d*/.test(await kutu.innerText());
}

/**
 * Sıradaki adım: kullanıcı ekrana bakınca ne yapacağını görür. Bir sevkiyat baştan sona yürütülür; her adımda sırası gelen
 * rol "Sıra sizde" ve tek düğmeyi, diğer roller işi kimin yapacağını okur; ana sayfadaki "Sizi bekleyen işler" bölümü sıradaki
 * işi doğru role gösterir. Akışın kendisi (durumlar, roller, kurallar) değişmez; yalnızca görünür olur.
 */
test('Sıradaki adım: her rol sırasını, yapacağı işi ve tek düğmesini görür', async ({ giris }) => {
  const depo = await giris('depo');
  const mudur = await giris('mudur');
  const fasonKalite = await giris('fason-kalite');
  for (const r of [depo, mudur, fasonKalite]) r.beklenen(/4\d\d (POST|PUT|PATCH) /);
  const iplik = damga();
  await iplikTanimla(mudur, iplik);
  const id = await taslakAc(mudur, iplik, '100');

  await mudur.adim('taslak: adım çubuğu altı adımı ve yapanı gösterir', async () => {
    await mudur.sayfa.goto(`/panel/sevkiyat/${id}`);
    const adimlar = mudur.sayfa
      .getByRole('list', { name: 'Sevkiyat süreci' })
      .getByRole('listitem');
    await expect(adimlar).toHaveCount(6);
    const metin = (await adimlar.allInnerTexts()).map((m) => m.replace(/\s+/g, ' '));
    for (const [sira, ad, yapan] of [
      [0, 'Yeni sevkiyat', 'Müdür'],
      [1, 'Çuvallar', 'Depo'],
      [2, 'Çıkış tartısı', 'Depo'],
      [3, 'İrsaliye', 'Müdür'],
      [4, 'Kumaş girişi', 'Kalite'],
      [5, 'Fire dosyası', 'Müdür'],
    ] as const) {
      mudur.kontrol(
        `${String(sira + 1)}. adım "${ad}" ve yapan "${yapan}" yazmalı`,
        (metin[sira] ?? '').includes(ad) && (metin[sira] ?? '').includes(yapan),
        metin[sira],
      );
    }
    await expect(adimlar.nth(1)).toHaveAttribute('aria-current', 'step');
  });

  await mudur.adim('taslak: müdür sıranın depoda olduğunu okur, düğme görmez', async () => {
    const metin = await siradakiAdim(mudur, id);
    mudur.kontrol('"Çuvallar eklenecek" yazmalı', /Çuvallar eklenecek/.test(metin), metin);
    mudur.kontrol('"Bu adımı Depo yapacak." yazmalı', /Bu adımı Depo yapacak\./.test(metin), metin);
    mudur.kontrol('müdürde "Sıra sizde" yazmamalı', !/Sıra sizde/.test(metin), metin);
    await expect(mudur.sayfa.getByTestId('guide-action')).toHaveCount(0);
  });

  await depo.adim(
    'taslak: depo "Sıra sizde" ve "Çuval eklemeye başla" düğmesini görür',
    async () => {
      depo.kontrol(
        'ana sayfada "Yüklenecek sevkiyat" listesinde olmalı',
        await anaSayfadaVar(depo, 'pending-dispatches'),
      );
      const metin = await siradakiAdim(depo, id);
      depo.kontrol('"Sıra sizde" yazmalı', /Sıra sizde/.test(metin), metin);
      depo.kontrol('"Çuvalları ekleyin" yazmalı', /Çuvalları ekleyin/.test(metin), metin);
      await expect(depo.sayfa.getByTestId('guide-action')).toHaveCount(1);
      await depo.sayfa.getByRole('button', { name: 'Çuval eklemeye başla' }).click();
      await expect(depo.sayfa.getByRole('tab', { name: 'Çuvallar', exact: true })).toHaveAttribute(
        'aria-selected',
        'true',
      );
      await expect(depo.sayfa.getByLabel(/Çuval kilosu/)).toBeVisible();
    },
  );

  await cuvalEkle(depo, id, 2, '50');

  await depo.adim('çuvallar eklendi: sıradaki adım çıkış tartısı olur', async () => {
    const metin = await siradakiAdim(depo, id);
    depo.kontrol('"Çıkış tartısını girin" yazmalı', /Çıkış tartısını girin/.test(metin), metin);
    await expect(
      depo.sayfa.getByTestId('next-step').getByRole('link', { name: 'Çıkış tartısını gir' }),
    ).toBeVisible();
  });

  await tartiGir(depo, id, '103,5', '3,5', '2');
  await depo.sayfa.waitForURL(new RegExp(`${id}$`), { timeout: 15_000 });

  await depo.adim('yolda: depo sıranın müdürde olduğunu okur', async () => {
    const metin = await siradakiAdim(depo, id);
    depo.kontrol(
      '"Bu adımı Müdür yapacak." yazmalı',
      /Bu adımı Müdür yapacak\./.test(metin),
      metin,
    );
    await expect(depo.sayfa.getByTestId('guide-action')).toHaveCount(0);
  });

  await mudur.adim('yolda: müdür ana sayfada irsaliyesi beklenen sevkiyatı görür', async () => {
    mudur.kontrol(
      'ana sayfada "İrsaliyesi beklenen sevkiyat" listesinde olmalı',
      await anaSayfadaVar(mudur, 'pending-deliveries'),
    );
    const metin = await siradakiAdim(mudur, id);
    mudur.kontrol('"Sıra sizde" yazmalı', /Sıra sizde/.test(metin), metin);
    await mudur.sayfa.getByRole('button', { name: 'İrsaliyeyi yükle' }).click();
    await expect(mudur.sayfa.getByRole('button', { name: 'Sevkiyatı tamamla' })).toBeInViewport();
  });

  await tamamla(mudur, id, `${iplik}-IRS`);

  await fasonKalite.adim('teslim edildi: fasoncu kalite kumaş girişi sırasını görür', async () => {
    fasonKalite.kontrol(
      'ana sayfada "Kumaşı beklenen sevkiyat" listesinde olmalı',
      await anaSayfadaVar(fasonKalite, 'pending-fabric'),
    );
    const metin = await siradakiAdim(fasonKalite, id);
    fasonKalite.kontrol('"Gelen kumaşı girin" yazmalı', /Gelen kumaşı girin/.test(metin), metin);
    await fasonKalite.sayfa.getByRole('button', { name: 'Kumaş girişi yap' }).click();
    await expect(
      fasonKalite.sayfa.getByRole('tab', { name: 'Kumaş girişi', exact: true }),
    ).toHaveAttribute('aria-selected', 'true');
  });

  await depo.adim(
    'teslim edildi: depo fire dosyasını okumaz, "Fasoncuya teslim edildi" görür',
    async () => {
      const metin = await siradakiAdim(depo, id);
      depo.kontrol(
        '"Fasoncuya teslim edildi" yazmalı',
        /Fasoncuya teslim edildi/.test(metin),
        metin,
      );
    },
  );

  // 100 kg iplik, %3 fire: 97 kg kumaş beklenir; 90 kg gelince 7 kg faturalanacak kalır.
  await kumasGir(fasonKalite, id, '90', `${iplik}-K`);

  await mudur.adim(
    'kumaş girildi: müdür fatura sırasını ve Faturalama düğmesini görür',
    async () => {
      mudur.kontrol(
        'ana sayfada "Faturası beklenen dosya" listesinde olmalı',
        await anaSayfadaVar(mudur, 'pending-invoices'),
      );
      const metin = await siradakiAdim(mudur, id);
      mudur.kontrol('"Faturayı girin" yazmalı', /Faturayı girin/.test(metin), metin);
      mudur.kontrol('faturalanacak kilo "7 kg" yazmalı', /7 kg faturalanacak/.test(metin), metin);
      await mudur.sayfa.getByRole('link', { name: 'Faturalama ekranına git' }).first().click();
      await mudur.sayfa.waitForURL(/\/panel\/faturalama/);
    },
  );

  await fasonKalite.adim('kumaş girildi: kalite sıranın müdürde olduğunu okur', async () => {
    const metin = await siradakiAdim(fasonKalite, id);
    fasonKalite.kontrol(
      '"Bu adımı Müdür yapacak." yazmalı',
      /Bu adımı Müdür yapacak\./.test(metin),
      metin,
    );
  });
});

/** Boş ekranlar ne yapılacağını söyler: süzgece uyan sevkiyat yoksa liste bunu ve çıkış yolunu gösterir. */
test('Sıradaki adım: boş sevkiyat listesi ne yapılacağını söyler', async ({ giris }) => {
  const mudur = await giris('mudur');
  await mudur.adim('süzgeçte kayıt yoksa "Tüm durumları göster" süzgeci kaldırır', async () => {
    await mudur.sayfa.goto('/panel/sevkiyat?durum=WEIGHT_MISMATCH');
    await mudur.sayfa.waitForLoadState('networkidle');
    const bos = mudur.sayfa.getByText('Bu durumda sevkiyat yok.');
    if ((await bos.count()) === 0) return; // Ortamda kantar uyuşmazlığında sevkiyat varsa liste doludur.
    await mudur.sayfa.getByRole('button', { name: 'Tüm durumları göster' }).click();
    await expect(mudur.sayfa).not.toHaveURL(/durum=/);
  });
});
