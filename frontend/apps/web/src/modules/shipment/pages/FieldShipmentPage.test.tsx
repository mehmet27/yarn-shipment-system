import { http, HttpResponse } from 'msw';
import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

describe('FieldShipmentListPage', () => {
  it('fasoncuya yalnızca kendisine giden sevkiyatları gösterir', async () => {
    server.use(...createHandlers('SUBCONTRACTOR'));
    renderApp('/saha');
    expect(await screen.findByRole('link', { name: 'SVK-2026-0001' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'SVK-2026-0002' })).not.toBeInTheDocument();
  });
});

describe('FieldShipmentPage', () => {
  it('teslim bekleyen sevkiyatta durum ve çıkış bilgisi görünür', async () => {
    server.use(...createHandlers('SUBCONTRACTOR'));
    renderApp('/saha/sevkiyat/shp-0010');
    expect(await screen.findByTestId('shipment-status')).toHaveTextContent('Fasoncuya yolda');
    expect(screen.getByText('SVK-2026-0010')).toBeInTheDocument();
  });

  it('başkasının sevkiyatında yetki uyarısı gösterir', async () => {
    server.use(...createHandlers('SUBCONTRACTOR'));
    renderApp('/saha/sevkiyat/shp-0002');
    expect(await screen.findByRole('alert')).toHaveTextContent('Bu sayfayı görme yetkiniz yok.');
  });

  it('geçersiz sevkiyat kimliği için "bulunamadı" gösterir', async () => {
    server.use(
      ...createHandlers('SUBCONTRACTOR'),
      http.get('/api/v1/shipments/:id', () =>
        HttpResponse.json(
          { status: 400, code: 'VALIDATION_FAILED', title: 'Geçersiz kimlik' },
          { status: 400 },
        ),
      ),
    );
    renderApp('/saha/sevkiyat/gecersiz');
    expect(await screen.findByText(/bulunamadı/i)).toBeInTheDocument();
    expect(screen.queryByText(/Beklenmeyen bir hata/)).not.toBeInTheDocument();
  });

  it('sevkiyat tamamlanmadan itiraz isteği atmaz', async () => {
    let claimRequests = 0;
    server.use(
      ...createHandlers('SUBCONTRACTOR'),
      http.get('/api/v1/quality-claims/shipments/:id', () => {
        claimRequests += 1;
        return HttpResponse.json(
          { status: 409, code: 'QUALITY_CLAIM_NOT_AVAILABLE', title: 'x' },
          { status: 409 },
        );
      }),
    );
    renderApp('/saha/sevkiyat/shp-0013');
    expect(await screen.findByText('SVK-2026-0013')).toBeInTheDocument();
    expect(claimRequests).toBe(0);
  });
});
