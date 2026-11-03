import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

describe('Uyuşmazlık ekranları', () => {
  it('listeyi önem rozetiyle gösterir ve kartı detaya bağlar', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp('/panel/uyusmazliklar');
    const card = await screen.findByRole('link', { name: 'UY-2026-0001' });
    expect(card).toHaveAttribute('href', '/panel/uyusmazliklar/disc-0');
    expect(within(card).getByText('Kırmızı')).toBeInTheDocument();
  });

  it('durum filtresini adrese yazar', async () => {
    server.use(...createHandlers('ADMIN'));
    const { router } = renderApp('/panel/uyusmazliklar');
    await userEvent.click(await screen.findByRole('combobox', { name: 'Durum' }));
    await userEvent.click(await screen.findByRole('option', { name: 'Çözüldü' }));
    expect(router.state.location.search).toBe('?durum=RESOLVED');
  });

  it('açık uyuşmazlığı bana atar, sonra notla karara bağlar', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp('/panel/uyusmazliklar/disc-0');
    await userEvent.click(await screen.findByRole('button', { name: 'Bana ata' }));
    await userEvent.click(await screen.findByRole('combobox', { name: /^Karar/ }));
    await userEvent.click(await screen.findByRole('option', { name: 'Tolerans içinde' }));
    await userEvent.type(screen.getByLabelText(/^Not/), 'Tolerans içinde kabul');
    await userEvent.click(screen.getByRole('button', { name: 'Karara bağla' }));
    expect(await screen.findAllByText('Tolerans içinde kabul')).toHaveLength(2);
    expect(screen.queryByRole('button', { name: 'Karara bağla' })).not.toBeInTheDocument();
  });

  it('fiş panelinde okunan ve girilen neti gösterir, okumayı "AI yanıldı" diye işaretler', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp('/panel/uyusmazliklar/disc-0');
    const dispatch = await screen.findByTestId('ticket-disc-0-dispatch-ticket');
    expect(within(dispatch).getByText('Çıkış kantarı')).toBeInTheDocument();
    expect(within(dispatch).getByText(/^Brüt 4\.500/)).toBeInTheDocument();
    expect(within(dispatch).getByText('+10 kg')).toBeInTheDocument();
    expect(
      await within(dispatch).findByRole('img', { name: 'Kantar fişi fotoğrafı' }),
    ).toBeInTheDocument();
    const delivery = screen.getByTestId('ticket-disc-0-delivery-ticket');
    expect(within(delivery).getByText('Kilolar okunamadı.')).toBeInTheDocument();
    await userEvent.click(within(dispatch).getByRole('button', { name: 'AI yanıldı' }));
    expect(await within(dispatch).findByText(/olarak işaretlendi/)).toBeInTheDocument();
    expect(within(dispatch).queryByRole('button', { name: 'AI yanıldı' })).not.toBeInTheDocument();
  });

  it('karar yetkisi olmayan kullanıcıya "AI yanıldı" düğmesini göstermez', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/uyusmazliklar/disc-0');
    const dispatch = await screen.findByTestId('ticket-disc-0-dispatch-ticket');
    expect(within(dispatch).queryByRole('button', { name: 'AI yanıldı' })).not.toBeInTheDocument();
  });
});
