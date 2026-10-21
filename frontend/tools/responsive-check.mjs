// FE-UI-07: mobil öncelikli denetim. Sahte API açık geliştirme sunucusunda (pnpm dev) çalışır:
//   pnpm check:responsive [--shots klasör]
// Her ekranı 360/768/1280 px'te açar; yatay kaydırma (hepsinde) ve 360 px'te 44 px'ten küçük
// dokunma hedefi bulursa 1 ile çıkar.
import { chromium } from 'playwright-core';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:5173';
const MIN_TARGET_PX = 44;
const TOUCH_MAX_WIDTH_PX = 768;
const WIDTHS = [360, 768, 1280];
const SCREENS = [
  ['WAREHOUSE', '/panel/sevkiyat'],
  ['WAREHOUSE', '/panel/sevkiyat/shp-0001'],
  ['WAREHOUSE', '/panel/sevkiyat/shp-0001/cikis'],
  ['QUALITY', '/panel/sevkiyat/shp-0007'],
  ['DRIVER', '/saha'],
  ['DRIVER', '/saha/sevkiyat/shp-0010'],
  ['SUBCONTRACTOR', '/saha'],
  ['SUBCONTRACTOR', '/saha/sevkiyat/shp-0004'],
  ['SUBCONTRACTOR', '/saha/sevkiyat/shp-0004/teslim'],
];
const shotsDir = process.argv.includes('--shots')
  ? process.argv[process.argv.indexOf('--shots') + 1]
  : undefined;

/** Tarayıcı içinde çalışır: görünen etkileşimli öğelerden küçük olanları listeler. */
function findSmallTargets(minPx) {
  const selector = 'a[href], button, input:not([type=hidden]), select, [role=tab], [role=combobox]';
  return [...document.querySelectorAll(selector)]
    .filter(
      (el) =>
        el.getClientRects().length > 0 &&
        el.getBoundingClientRect().width > 0 &&
        getComputedStyle(el).visibility !== 'hidden',
    )
    .map((el) => ({ el, rect: el.getBoundingClientRect() }))
    .filter(({ rect }) => rect.height < minPx || rect.width < minPx)
    .map(
      ({ el, rect }) =>
        `${el.tagName.toLowerCase()} "${(el.textContent ?? el.getAttribute('aria-label') ?? '').trim().slice(0, 30)}" ${Math.round(rect.width)}x${Math.round(rect.height)}`,
    );
}

const browser = await chromium.launch({
  ...(process.env.CHROMIUM_PATH
    ? { executablePath: process.env.CHROMIUM_PATH }
    : { channel: 'chrome' }),
  args: ['--no-sandbox'],
});
const problems = [];
for (const width of WIDTHS) {
  for (const [role, path] of SCREENS) {
    const page = await browser.newPage({ viewport: { width, height: 800 }, hasTouch: width < 768 });
    await page.goto(`${BASE_URL}${path}?mockRole=${role}`);
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(500);
    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - window.innerWidth,
    );
    // Dokunma hedefi kuralı dokunmatik genişliklerde (< 768 px) geçerlidir; fare için gevşektir.
    const small =
      width < TOUCH_MAX_WIDTH_PX ? await page.evaluate(findSmallTargets, MIN_TARGET_PX) : [];
    const label = `${width}px ${role} ${path}`;
    if (overflow > 0) problems.push(`${label}: yatay taşma ${overflow}px`);
    for (const item of small) problems.push(`${label}: küçük hedef ${item}`);
    if (shotsDir) {
      const name = `${width}-${role}-${path.replaceAll('/', '_')}`;
      await page.screenshot({ path: `${shotsDir}/${name}.png`, fullPage: true });
    }
    await page.close();
  }
}
await browser.close();
console.log(problems.length === 0 ? 'Duyarlılık denetimi temiz.' : problems.join('\n'));
process.exit(problems.length === 0 ? 0 : 1);
