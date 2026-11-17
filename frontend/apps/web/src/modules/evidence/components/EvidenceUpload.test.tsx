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

  it('müdüre taslak sevkiyatta itiraz kanıtı sunulmaz', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat/shp-0001');
    await screen.findByText('Kilolar');
    expect(screen.queryByRole('button', { name: 'Fotoğraf çek' })).not.toBeInTheDocument();
  });

  it('müdüre tamamlanmış sevkiyatta itiraz kanıtı sunulur', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat/shp-0003');
    expect(await screen.findByRole('button', { name: 'Fotoğraf çek' })).toBeInTheDocument();
  });

  it('birden çok belge türü izinliyse tür seçilmeden yükleme düğmeleri pasiftir', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat/shp-0003');
    const button = await screen.findByRole('button', { name: 'Fotoğraf çek' });
    expect(screen.getByText('Yüklenecek belge ne?')).toBeInTheDocument();
    expect(button).toBeDisabled();
  });
});
