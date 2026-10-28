import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

const READABLE_BYTES = 2000;
const UNREADABLE_BYTES = 10;

/** Fişi kameradan çekmiş gibi yükler ve gönderir; küçük dosya mock'ta "okunamadı" döner. */
export async function captureTicket(isReadable = true, testId = 'ticket-input') {
  const size = isReadable ? READABLE_BYTES : UNREADABLE_BYTES;
  const file = new File([new Uint8Array(size)], 'fis.jpg', { type: 'image/jpeg' });
  await userEvent.upload(await screen.findByTestId(testId), file);
  await userEvent.click(await screen.findByRole('button', { name: 'Fotoğrafı gönder' }));
}
