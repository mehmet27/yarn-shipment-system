import { SCREENS } from '../screens';
import {
  clippedButtonLabels,
  horizontalOverflow,
  offCenterButtonLabels,
  pageHeadings,
  smallTouchTargets,
} from '../support/layout';
import { expect, test } from '../support/test';

const TOUCH_MAX_WIDTH_PX = 768;

// Her ekran her genişlikte: hatasız açılır, taşmaz, dokunma hedefleri yeterince büyüktür.
for (const screen of SCREENS) {
  test(
    `${screen.name} açılır ve düzeni bozulmaz`,
    { tag: ['@ekran', '@smoke'] },
    async ({ page, openAs }) => {
      await openAs(screen.role, screen.path);
      await expect(page.getByText(screen.ready).first()).toBeVisible();

      expect(await horizontalOverflow(page), 'yatay taşma (px)').toBe(0);
      expect(await clippedButtonLabels(page), 'kesik düğme yazısı').toEqual([]);
      expect(await offCenterButtonLabels(page), 'dikeyde ortalanmamış düğme yazısı').toEqual([]);
      expect(await pageHeadings(page), 'tek sayfa başlığı (h1)').toHaveLength(1);
      const width = page.viewportSize()?.width ?? TOUCH_MAX_WIDTH_PX;
      if (width < TOUCH_MAX_WIDTH_PX) {
        expect(await smallTouchTargets(page), 'küçük dokunma hedefi').toEqual([]);
      }
    },
  );
}
