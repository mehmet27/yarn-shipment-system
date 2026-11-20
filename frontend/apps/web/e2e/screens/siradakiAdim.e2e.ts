import { addBags } from '../support/bags';
import { expect, test } from '../support/test';

// Her ekranda kullanıcı ne yapacağını görür: sürecin adımları, sıra kimde ve tek "sonraki işlem" düğmesi.
test.describe('Sıradaki adım', { tag: '@ekran' }, () => {
  test('depo taslakta sırayla çuval ekler, sonra çıkış tartısı düğmesini görür', async ({
    page,
    openAs,
  }) => {
    await openAs('WAREHOUSE', '/panel/sevkiyat/shp-0001');
    const card = page.getByTestId('next-step');
    await expect(card.getByText('Sıra sizde')).toBeVisible();
    await expect(card.getByText('Çuvalları ekleyin', { exact: true })).toBeVisible();
    await expect(page.getByTestId('guide-action')).toHaveCount(1);
    await card.getByRole('button', { name: 'Çuval eklemeye başla' }).click();
    await expect(page.getByRole('tab', { name: 'Çuvallar' })).toHaveAttribute(
      'aria-selected',
      'true',
    );
    await expect(page.getByLabel('Çuval kilosu (kg)')).toBeInViewport();
    await addBags(page);
    await expect(card.getByText('Çıkış tartısını girin')).toBeVisible();
    await expect(card.getByRole('link', { name: 'Çıkış tartısını gir' })).toBeVisible();
  });

  test('müdür taslakta sıranın depoda olduğunu okur', async ({ page, openAs }) => {
    await openAs('MANAGER', '/panel/sevkiyat/shp-0001');
    const card = page.getByTestId('next-step');
    await expect(card.getByText('Bu adımı Depo yapacak.', { exact: false })).toBeVisible();
    await expect(page.getByTestId('guide-action')).toHaveCount(0);
  });

  test('müdür yoldaki sevkiyatta "İrsaliyeyi yükle" ile forma gider', async ({ page, openAs }) => {
    await openAs('MANAGER', '/panel/sevkiyat/shp-0002');
    await page.getByTestId('next-step').getByRole('button', { name: 'İrsaliyeyi yükle' }).click();
    await expect(page.getByRole('button', { name: 'Sevkiyatı tamamla' })).toBeInViewport();
  });

  test('kalite tamamlanmış sevkiyatta "Kumaş girişi yap" ile sekmeyi açar', async ({
    page,
    openAs,
  }) => {
    await openAs('QUALITY', '/panel/sevkiyat/shp-0007');
    await page.getByTestId('next-step').getByRole('button', { name: 'Kumaş girişi yap' }).click();
    await expect(page.getByRole('tab', { name: 'Kumaş girişi' })).toHaveAttribute(
      'aria-selected',
      'true',
    );
  });

  test('telefonda adımlar özet satırıdır; "Tüm adımlar" listeyi açar', async ({
    page,
    openAs,
  }, testInfo) => {
    test.skip(testInfo.project.name !== 'telefon-360', 'yalnızca dar ekran');
    await openAs('WAREHOUSE', '/panel/sevkiyat/shp-0001');
    await expect(page.getByText('2 / 6 adım: Çuvallar')).toBeVisible();
    await page.getByRole('button', { name: 'Tüm adımlar' }).click();
    await expect(
      page.getByRole('list', { name: 'Sevkiyat süreci' }).getByRole('listitem'),
    ).toHaveCount(6);
  });

  test('ana sayfa: müdür irsaliyesi beklenen sevkiyatı, kalite kumaşı beklenen sevkiyatı görür', async ({
    page,
    openAs,
  }) => {
    await openAs('MANAGER', '/panel');
    await expect(page.getByTestId('pending-deliveries')).toContainText(
      /İrsaliyesi beklenen sevkiyat: \d+/,
    );
    await openAs('QUALITY', '/panel');
    await expect(page.getByTestId('pending-fabric')).toContainText(/Kumaşı beklenen sevkiyat: \d+/);
  });
});
