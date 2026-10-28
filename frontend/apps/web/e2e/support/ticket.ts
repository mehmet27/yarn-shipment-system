import type { Page } from '@playwright/test';

const READABLE_BYTES = 2000;
const UNREADABLE_BYTES = 10;

/** Kantar fişini kameradan çekmiş gibi yükler; küçük dosya sahte API'de "okunamadı" döner. */
export async function captureTicket(page: Page, isReadable = true) {
  const size = isReadable ? READABLE_BYTES : UNREADABLE_BYTES;
  await page.getByTestId('ticket-input').setInputFiles({
    name: 'fis.jpg',
    mimeType: 'image/jpeg',
    buffer: Buffer.alloc(size),
  });
  await page.getByRole('button', { name: 'Fotoğrafı gönder' }).click();
}
