import type { Page } from '@playwright/test';

/** Açılır listeden (Mantine Select) seçim yapar; liste kutu dışına açıldığı için görünür seçenek aranır. */
export async function sec(
  sayfa: Page,
  etiket: string | RegExp,
  secenek: string,
  kapsam: Page | ReturnType<Page['getByRole']> = sayfa,
) {
  await kapsam.getByLabel(etiket).first().click();
  await sayfa.locator('[role=option]:visible').filter({ hasText: secenek }).first().click();
}

/** Sunucuya yüklenebilecek, sahte ama gerçekçi bir JPEG (kantar fişi, irsaliye, itiraz fotoğrafı). */
export async function resim(sayfa: Page, tohum = 1): Promise<Buffer> {
  // Her fotoğraf benzersiz olmalı: aynı dosyanın yeniden yüklenmesi sistemde 'kanıt tekrar kullanımı' olayı üretir.
  const benzersiz = tohum + Math.floor(Math.random() * 100_000);
  const veri = await sayfa.evaluate((t) => {
    const tuval = document.createElement('canvas');
    tuval.width = 1000;
    tuval.height = 1400;
    const x = tuval.getContext('2d');
    if (x === null) return '';
    x.fillStyle = '#fff';
    x.fillRect(0, 0, 1000, 1400);
    x.fillStyle = '#000';
    x.font = '26px monospace';
    let s = t;
    for (let i = 0; i < 50; i += 1) {
      s = (s * 9301 + 49297) % 233280;
      x.fillText(
        `ROBOT BELGE ${String(i)} ${String(Math.floor((s / 233280) * 900))}`,
        60,
        50 + i * 25,
      );
    }
    return tuval.toDataURL('image/jpeg', 0.9);
  }, benzersiz);
  return Buffer.from(veri.split(',')[1] ?? '', 'base64');
}

/** Her koşuda benzersiz, robotun ürettiğini belli eden ek. */
export const damga = () => `ROBOT-${new Date().toISOString().slice(5, 16).replace(/\D/g, '')}`;

/** Metni içeren, düğmesi olan en iç satır/kart kutusu (liste satırlarının ortak yapısı yok). */
export function satir(sayfa: Page, metin: string, dugme: string | RegExp) {
  return sayfa
    .locator('main div')
    .filter({ hasText: metin })
    .filter({ has: sayfa.getByRole('button', { name: dugme }) })
    .last();
}

/** Küçük, geçerli bir PNG (logo yükleme denemeleri). */
export async function logoPng(sayfa: Page): Promise<Buffer> {
  const veri = await sayfa.evaluate(() => {
    const tuval = document.createElement('canvas');
    tuval.width = 120;
    tuval.height = 40;
    const x = tuval.getContext('2d');
    if (x === null) return '';
    x.fillStyle = '#1c4e80';
    x.fillRect(0, 0, 120, 40);
    x.fillStyle = '#fff';
    x.font = '20px sans-serif';
    x.fillText('ROBOT', 20, 27);
    return tuval.toDataURL('image/png');
  });
  return Buffer.from(veri.split(',')[1] ?? '', 'base64');
}
