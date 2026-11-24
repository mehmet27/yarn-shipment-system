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
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/uyusmazliklar');
    // İlk test sayfayı soğuk yükler; CI'da 1 sn'lik varsayılan bekleme yetmeyebilir.
    const card = await screen.findByRole('link', { name: 'UY-2026-0001' }, { timeout: 5000 });
    expect(card).toHaveAttribute('href', '/panel/uyusmazliklar/disc-0');
    expect(within(card).getByText('Kırmızı')).toBeInTheDocument();
  });

  it('durum filtresini adrese yazar', async () => {
    server.use(...createHandlers('MANAGER'));
    const { router } = renderApp('/panel/uyusmazliklar');
    await userEvent.click(await screen.findByRole('combobox', { name: 'Durum' }));
    await userEvent.click(await screen.findByRole('option', { name: 'Çözüldü' }));
    expect(router.state.location.search).toBe('?durum=RESOLVED');
  });

  it('süresi geçenler filtresini adrese yazar ve listeyi daraltır', async () => {
    server.use(...createHandlers('MANAGER'));
    const { router } = renderApp('/panel/uyusmazliklar');
    await screen.findByRole('link', { name: 'UY-2026-0001' });
    await userEvent.click(screen.getByRole('checkbox', { name: 'Yalnızca süresi geçenler' }));
    expect(router.state.location.search).toBe('?gecikmis=1');
    await screen.findByRole('checkbox', { name: 'Yalnızca süresi geçenler', checked: true });
  });

  it('kalite açık uyuşmazlığı bana atar ama karar veremez', async () => {
    server.use(...createHandlers('QUALITY'));
    renderApp('/panel/uyusmazliklar/disc-0');
    await userEvent.click(await screen.findByRole('button', { name: 'Bana ata' }));
    expect(await screen.findByText('Bu uyuşmazlık sizde.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Bana ata' })).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Karar ver' })).not.toBeInTheDocument();
  });

  it('müdür açık uyuşmazlığı notla karara bağlar; onay sorulur', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/uyusmazliklar/disc-0');
    expect(await screen.findByRole('heading', { name: 'Karar ver' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Bana ata' })).not.toBeInTheDocument();
    await userEvent.click(await screen.findByRole('combobox', { name: /^Karar/ }));
    expect(screen.queryByRole('option', { name: 'Tolerans içinde' })).not.toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'Sorumluluk atandı' })).not.toBeInTheDocument();
    await userEvent.click(await screen.findByRole('option', { name: 'Haksız' }));
    expect(screen.getByTestId('resolution-effect')).toHaveTextContent('yalnızca kapatılır');
    const note = screen.getByLabelText(/^Not/);
    expect(note.tagName).toBe('TEXTAREA');
    await userEvent.type(note, 'Talep haksız kabul');
    await userEvent.click(screen.getByRole('button', { name: 'Karara bağla' }));
    expect(screen.getByText(/Karar geri alınamaz\./)).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Vazgeç' }));
    expect(screen.getByRole('button', { name: 'Karara bağla' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Karara bağla' }));
    await userEvent.click(screen.getByRole('button', { name: 'Evet, kararı kaydet' }));
    expect(await screen.findAllByText('Talep haksız kabul')).toHaveLength(2);
    expect(screen.queryByRole('button', { name: 'Karara bağla' })).not.toBeInTheDocument();
  });

  it('haklı bulunan itirazda kilonun faturalanacağa eklendiğini söyler', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/uyusmazliklar/disc-1');
    await userEvent.click(await screen.findByRole('combobox', { name: /^Karar/ }));
    await userEvent.click(await screen.findByRole('option', { name: 'Haklı' }));
    expect(screen.getByTestId('resolution-effect')).toHaveTextContent(
      /faturalanacak kiloya eklenir/,
    );
  });

  it('karar notu zorunlu hatasını düz Türkçe gösterir', async () => {
    server.use(
      http.post(`${DISCREPANCIES_PATH}/disc-1/resolve`, () =>
        HttpResponse.json(
          { status: 400, code: 'DISCREPANCY_NOTE_REQUIRED', title: 'x' },
          { status: 400 },
        ),
      ),
      ...createHandlers('MANAGER'),
    );
    renderApp('/panel/uyusmazliklar/disc-1');
    await userEvent.click(await screen.findByRole('combobox', { name: /^Karar/ }));
    await userEvent.click(await screen.findByRole('option', { name: 'Haksız' }));
    await userEvent.type(screen.getByLabelText(/^Not/), 'x');
    await userEvent.click(screen.getByRole('button', { name: 'Karara bağla' }));
    await userEvent.click(screen.getByRole('button', { name: 'Evet, kararı kaydet' }));
    expect(await screen.findByText(/Karar notu boş olamaz/)).toBeInTheDocument();
  });

  it('müdür ana sayfada karar bekleyen uyuşmazlıkları listeler', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel');
    const block = await screen.findByTestId('pending-discrepancies');
    expect(block).toHaveTextContent('Karar bekleyen uyuşmazlıklar');
    expect(within(block).getAllByRole('link', { name: /^UY-2026-/ }).length).toBeLessThanOrEqual(5);
    expect(within(block).getByRole('link', { name: 'Tümünü gör' })).toHaveAttribute(
      'href',
      '/panel/uyusmazliklar?sayfa=1',
    );
  });

  it('kalite ana sayfada karar bekleyen uyuşmazlık listesini görmez (karar müdürde)', async () => {
    server.use(...createHandlers('QUALITY'));
    renderApp('/panel');
    await screen.findByRole('heading', { level: 1 });
    expect(screen.queryByTestId('pending-discrepancies')).not.toBeInTheDocument();
  });

  it('depo ana sayfada karar bekleyen uyuşmazlık listesini görmez', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel');
    await screen.findByTestId('pending-dispatches');
    expect(screen.queryByTestId('pending-discrepancies')).not.toBeInTheDocument();
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
      ...createHandlers('MANAGER'),
    );
    renderApp('/panel/uyusmazliklar/disc-0');
    expect(await screen.findByText(/· Kerem Kalite$/)).toBeInTheDocument();
    expect(screen.getByText('Melek Müdür')).toBeInTheDocument();
    expect(screen.queryByText(/Bir çalışan/)).not.toBeInTheDocument();
  });
});
