import type { Page } from '@playwright/test';

/** FE-UI-07: dokunmatik genişliklerde etkileşimli öğe en az bu boyda olur. */
export const MIN_TARGET_PX = 44;

/** Sayfa pencereden genişse taşan piksel sayısı (0 = taşma yok). */
export function horizontalOverflow(page: Page) {
  return page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
}

/** Görünen etkileşimli öğelerden küçük olanları "etiket GxY" biçiminde listeler. */
export function smallTouchTargets(page: Page) {
  return page.evaluate((minPx) => {
    const selector =
      'a[href], button, input:not([type=hidden]), select, [role=tab], [role=combobox]';
    return [...document.querySelectorAll(selector)]
      .map((el) => ({ el, rect: el.getBoundingClientRect() }))
      .filter(({ el, rect }) => rect.width > 0 && getComputedStyle(el).visibility !== 'hidden')
      .filter(({ rect }) => rect.height < minPx || rect.width < minPx)
      .map(({ el, rect }) => {
        const label = (el.getAttribute('aria-label') ?? el.textContent).trim().slice(0, 30);
        return `${el.tagName.toLowerCase()} "${label}" ${String(Math.round(rect.width))}x${String(Math.round(rect.height))}`;
      });
  }, MIN_TARGET_PX);
}

/** Yazısı kapsayıcısına sığmayan (kesilmiş) düğmeler. */
export function clippedButtonLabels(page: Page) {
  return page.evaluate(() =>
    [...document.querySelectorAll('.mantine-Button-label')]
      .filter((el) => el.scrollWidth > el.clientWidth + 1)
      .map((el) => el.textContent.slice(0, 40)),
  );
}

/** Yazısı düğmenin dikey ortasından kaymış düğmeler (H-26); 2 px'e kadar fark yuvarlamadır. */
export function offCenterButtonLabels(page: Page) {
  return page.evaluate(() =>
    [...document.querySelectorAll('.mantine-Button-root')]
      .map((root) => ({ root, label: root.querySelector('.mantine-Button-label') }))
      .filter(({ root, label }) => label !== null && root.getBoundingClientRect().height > 0)
      .filter(({ root, label }) => {
        const box = root.getBoundingClientRect();
        const text = label?.getBoundingClientRect() ?? box;
        return Math.abs(box.top + box.height / 2 - (text.top + text.height / 2)) > 2;
      })
      .map(({ root }) => root.textContent.trim().slice(0, 40)),
  );
}

/** Sayfa başlığı (h1); ekran okuyucu ve testler sayfayı bununla tanır (H-28). */
export function pageHeadings(page: Page) {
  return page.evaluate(() =>
    [...document.querySelectorAll('h1')].map((el) => el.textContent.trim()),
  );
}
