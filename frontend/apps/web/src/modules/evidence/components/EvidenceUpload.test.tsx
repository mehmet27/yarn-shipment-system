import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

describe('EvidenceUpload', () => {
  it('izni olan kullanıcıya kamera ve dosya düğmelerini gösterir', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat/shp-0001');
    expect(await screen.findByRole('button', { name: 'Fotoğraf çek' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Dosya seç' })).toBeInTheDocument();
  });

  it('yükleme izni olmayan kullanıcıda hiç görünmez', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat/shp-0007');
    await screen.findByText('Kilolar');
    expect(screen.queryByRole('button', { name: 'Fotoğraf çek' })).not.toBeInTheDocument();
  });
});
