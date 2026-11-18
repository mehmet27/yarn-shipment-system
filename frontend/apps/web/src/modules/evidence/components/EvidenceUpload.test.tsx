import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { http, HttpResponse } from 'msw';
import { createHandlers } from '@/mocks/handlers';
import { createMockUser } from '@/mocks/users';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

describe('EvidenceUpload', () => {
  it('çıkış tartısı girilmiş sevkiyatta depoya kamera ve dosya düğmelerini gösterir', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat/shp-0045');
    expect(await screen.findByRole('button', { name: 'Fotoğraf çek' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Dosya seç' })).toBeInTheDocument();
  });

  it('çıkış tartısı girilmeden depoya fotoğraf bölümü sunulmaz; fiş tartıyla birlikte yüklenir', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat/shp-0001');
    await screen.findByRole('heading', { name: 'SVK-2026-0001' });
    expect(screen.queryByRole('button', { name: 'Fotoğraf çek' })).not.toBeInTheDocument();
  });

  it('kantar fişi yüklenmiş ve yola çıkmış sevkiyatta depoya yenileme bölümü sunulmaz', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat/shp-0007');
    await screen.findByText('Kilolar');
    await screen.findByText(/Kantar fişi|Fotoğraf ve belgeler/);
    expect(screen.queryByText('Kantar fişini yenile')).not.toBeInTheDocument();
  });

  it('yükleme izni olmayan kullanıcıda hiç görünmez', async () => {
    server.use(...createHandlers('MANAGER'));
    const user = createMockUser('MANAGER');
    server.use(
      http.get('/api/v1/me', () =>
        HttpResponse.json({
          ...user,
          permissions: user.permissions.filter(
            (permission) => !permission.startsWith('EVIDENCE_UPLOAD'),
          ),
        }),
      ),
    );
    renderApp('/panel/sevkiyat/shp-0007');
    await screen.findByText('Kilolar');
    expect(screen.queryByRole('button', { name: 'Fotoğraf çek' })).not.toBeInTheDocument();
  });

  it('müdüre ve kaliteye tamamlanmış sevkiyatta itiraz/laboratuvar yükleme alanı gelmez', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat/shp-0003');
    await screen.findByText('Kilolar');
    expect(screen.queryByText('Yüklenecek belge ne?')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Fotoğraf çek' })).not.toBeInTheDocument();
  });

  it('kalite kullanıcısında "Fotoğraf ve belgeler" bölümü görünmez', async () => {
    server.use(...createHandlers('QUALITY'));
    renderApp('/panel/sevkiyat/shp-0003');
    await screen.findByText('Kilolar');
    expect(screen.queryByText('Fotoğraf ve belgeler')).not.toBeInTheDocument();
    expect(screen.queryByText('Kantar fişini yenile')).not.toBeInTheDocument();
  });
});
