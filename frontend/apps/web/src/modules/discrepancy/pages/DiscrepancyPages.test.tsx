import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { DISCREPANCIES_PATH } from '@iplik/api-client';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

describe('Uyuşmazlık ekranları', () => {
  it('listeyi önem rozetiyle gösterir ve kartı detaya bağlar', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp('/panel/uyusmazliklar');
    // İlk test sayfayı soğuk yükler; CI'da 1 sn'lik varsayılan bekleme yetmeyebilir.
    const card = await screen.findByRole('link', { name: 'UY-2026-0001' }, { timeout: 5000 });
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

  it('süresi geçenler filtresini adrese yazar ve listeyi daraltır', async () => {
    server.use(...createHandlers('ADMIN'));
    const { router } = renderApp('/panel/uyusmazliklar');
    await screen.findByRole('link', { name: 'UY-2026-0001' });
    await userEvent.click(screen.getByRole('checkbox', { name: 'Yalnızca süresi geçenler' }));
    expect(router.state.location.search).toBe('?gecikmis=1');
    await screen.findByRole('checkbox', { name: 'Yalnızca süresi geçenler', checked: true });
  });

  it('açık uyuşmazlığı bana atar, sonra notla karara bağlar', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp('/panel/uyusmazliklar/disc-0');
    await userEvent.click(await screen.findByRole('button', { name: 'Bana ata' }));
    await userEvent.click(await screen.findByRole('combobox', { name: /^Karar/ }));
    expect(screen.queryByRole('option', { name: 'Tolerans içinde' })).not.toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'Veri hatası' })).not.toBeInTheDocument();
    await userEvent.click(await screen.findByRole('option', { name: 'Talep haksız' }));
    await userEvent.type(screen.getByLabelText(/^Not/), 'Talep haksız kabul');
    await userEvent.click(screen.getByRole('button', { name: 'Karara bağla' }));
    expect(await screen.findAllByText('Talep haksız kabul')).toHaveLength(2);
    expect(screen.queryByRole('button', { name: 'Karara bağla' })).not.toBeInTheDocument();
  });

  it('geçmişte işlemi yapanın ve atananın ad soyadını gösterir', async () => {
    server.use(
      http.get(`${DISCREPANCIES_PATH}/disc-0/activity`, () =>
        HttpResponse.json([
          {
            id: 'a1',
            actionType: 'ASSIGNED',
            actorId: 'user-quality',
            actorName: 'Kerem Kalite',
            assigneeName: 'Melek Müdür',
            note: 'user-manager',
            occurredAt: '2026-10-20T11:00:00Z',
          },
        ]),
      ),
      ...createHandlers('ADMIN'),
    );
    renderApp('/panel/uyusmazliklar/disc-0');
    expect(await screen.findByText(/· Kerem Kalite$/)).toBeInTheDocument();
    expect(screen.getByText('Melek Müdür')).toBeInTheDocument();
    expect(screen.queryByText(/Başka bir kullanıcı/)).not.toBeInTheDocument();
  });
});
