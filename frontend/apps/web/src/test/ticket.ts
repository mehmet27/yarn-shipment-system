import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

const PHOTO_BYTES = 2000;

/** Fişi kameradan çekmiş gibi yükler ve gönderir. */
export async function captureTicket(testId = 'ticket-input') {
  const file = new File([new Uint8Array(PHOTO_BYTES)], 'fis.jpg', { type: 'image/jpeg' });
  await userEvent.upload(await screen.findByTestId(testId), file);
  await userEvent.click(await screen.findByRole('button', { name: 'Fotoğrafı gönder' }));
}
