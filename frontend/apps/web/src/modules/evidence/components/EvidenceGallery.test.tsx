import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { http, HttpResponse } from 'msw';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

const SCALE = {
  id: 'e1',
  shipmentId: 'shp-0001',
  evidenceType: 'DISPATCH_SCALE',
  mimeType: 'image/jpeg',
  sizeBytes: 10,
  sha256: 'x',
  status: 'VALID',
  uploadedAt: '2026-11-10T09:00:00Z',
};

describe('Kanıtlar sekmesi', () => {
  it('yüklenen kanıtı türüyle listeler', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat/shp-0001');
    (await screen.findByRole('tab', { name: 'Kanıtlar' })).click();
    expect(
      await screen.findByRole('button', { name: /Çıkış tartısı fotoğrafını büyüt/ }),
    ).toBeInTheDocument();
  });

  it('geçersiz kılınmış kanıtı rozet ve gerekçeyle gösterir', async () => {
    server.use(...createHandlers('MANAGER'));
    server.use(
      http.get('/api/v1/evidence', () =>
        HttpResponse.json({
          items: [{ ...SCALE, status: 'INVALIDATED', invalidatedReason: 'Yanlış sevkiyat' }],
        }),
      ),
    );
    renderApp('/panel/sevkiyat/shp-0001');
    (await screen.findByRole('tab', { name: 'Kanıtlar' })).click();
    expect(await screen.findByText('Geçersiz')).toBeInTheDocument();
    expect(screen.getByText(/Yanlış sevkiyat/)).toBeInTheDocument();
  });

  it('kanıt yoksa boş durumu söyler', async () => {
    server.use(...createHandlers('MANAGER'));
    server.use(http.get('/api/v1/evidence', () => HttpResponse.json({ items: [] })));
    renderApp('/panel/sevkiyat/shp-0001');
    (await screen.findByRole('tab', { name: 'Kanıtlar' })).click();
    expect(
      await screen.findByText('Henüz yüklenmiş fotoğraf ya da belge yok.'),
    ).toBeInTheDocument();
  });
});

describe('Kanıt geçersiz kılma', () => {
  it('admin gerekçeyle geçersiz kılar, kanıt rozetle görünür', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp('/panel/sevkiyat/shp-0001');
    (await screen.findByRole('tab', { name: 'Kanıtlar' })).click();
    (await screen.findAllByRole('button', { name: 'Geçersiz kıl' }))[0]?.click();
    const reason = await screen.findByLabelText(/Gerekçe/);
    await userEvent.type(reason, 'Yanlış sevkiyatın fişi');
    const confirm = (await screen.findAllByRole('button', { name: 'Geçersiz kıl' })).at(-1);
    if (confirm === undefined) throw new Error('onay düğmesi yok');
    await userEvent.click(confirm);
    expect(await screen.findByText('Geçersiz')).toBeInTheDocument();
  });

  it('izni olmayan kullanıcıda düğme hiç görünmez', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat/shp-0001');
    (await screen.findByRole('tab', { name: 'Kanıtlar' })).click();
    await screen.findByRole('button', { name: /fotoğrafını büyüt/ });
    expect(screen.queryByRole('button', { name: 'Geçersiz kıl' })).not.toBeInTheDocument();
  });
});
