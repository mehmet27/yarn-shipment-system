import type { Page } from '@playwright/test';

const PHOTO_BYTES = 2000;

/** Kantar fişini kameradan çekmiş gibi yükler ve gönderir. */
export async function captureTicket(page: Page) {
  await page.getByTestId('ticket-input').setInputFiles({
    name: 'fis.jpg',
    mimeType: 'image/jpeg',
    buffer: Buffer.alloc(PHOTO_BYTES),
  });
  await page.getByRole('button', { name: 'Fotoğrafı gönder' }).click();
}
