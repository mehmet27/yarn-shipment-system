import type { Locator } from '@playwright/test';
import { expect, test } from '../support/test';

const TABLET_MIN_WIDTH_PX = 768;

/** Daraltılan menü ekran dışına kayar (gizlenmez), bu yüzden konuma bakılır. */
const onScreen = (link: Locator) => link.evaluate((el) => el.getBoundingClientRect().right > 0);

test('yan menü masaüstünde kapanıp açılır, mobilde bağlantıya tıklayınca kapanır @ekran', async ({
  page,
  openAs,
}) => {
  await openAs('ADMIN', '/panel/sevkiyat');
  const link = page.getByRole('link', { name: 'Tanımlar' });
  const toggle = page
    .getByRole('button', { name: 'Yan menü', exact: true })
    .and(page.locator(':visible'));
  const shown = (want: boolean) => expect.poll(() => onScreen(link)).toBe(want);
  if ((page.viewportSize()?.width ?? 0) < TABLET_MIN_WIDTH_PX) {
    await shown(false);
    await toggle.click();
    await shown(true);
    await link.click();
    await shown(false);
    return;
  }
  await shown(true);
  await toggle.click();
  await shown(false);
  await toggle.click();
  await shown(true);
});
