import { http, HttpResponse } from 'msw';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

describe('FieldShipmentListPage', () => {
  it('şoföre yalnızca kendisine atanan sevkiyatları gösterir', async () => {
    server.use(...createHandlers('DRIVER'));
    renderApp('/saha');
    expect(await screen.findByRole('link', { name: 'SVK-2026-0001' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'SVK-2026-0002' })).not.toBeInTheDocument();
  });
});

describe('FieldShipmentPage', () => {
  it('şoföre çuval sayısı ve mühür no gösterir, kilo göstermez', async () => {
    server.use(...createHandlers('DRIVER'));
    renderApp('/saha/sevkiyat/shp-0010');
    expect(await screen.findByText('M-0010')).toBeInTheDocument();
    expect(screen.getByText('40')).toBeInTheDocument();
    expect(screen.queryByText(/kg/)).not.toBeInTheDocument();
  });

  it('yükü teslim alınca durum Yolda olur ve buton kaybolur', async () => {
    server.use(...createHandlers('DRIVER'));
    renderApp('/saha/sevkiyat/shp-0010');
    await userEvent.click(await screen.findByRole('button', { name: 'Yükü teslim aldım' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Evet, teslim aldım' }));
    expect(await screen.findByText('Yolda')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Yükü teslim aldım' })).not.toBeInTheDocument();
  });

  it('teslim aldıktan sonra varışı mühür durumuyla beyan eder; durum Teslim bekliyor olur', async () => {
    server.use(...createHandlers('DRIVER'));
    renderApp('/saha/sevkiyat/shp-0010');
    await userEvent.click(await screen.findByRole('button', { name: 'Yükü teslim aldım' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Evet, teslim aldım' }));
    await userEvent.click(
      await screen.findByRole('button', { name: 'Vardım, mühür hasarlı veya yok' }),
    );
    expect(await screen.findByText('Teslim bekliyor')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Vardım, mühür sağlam' })).not.toBeInTheDocument();
  });

  it('başkasının sevkiyatında yetki uyarısı gösterir', async () => {
    server.use(...createHandlers('DRIVER'));
    renderApp('/saha/sevkiyat/shp-0002');
    expect(await screen.findByRole('alert')).toHaveTextContent('Bu sayfayı görme yetkiniz yok.');
  });

  it('teslim almadan önce onay sorar; vazgeçince durum değişmez', async () => {
    server.use(...createHandlers('DRIVER'));
    renderApp('/saha/sevkiyat/shp-0010');
    await userEvent.click(await screen.findByRole('button', { name: 'Yükü teslim aldım' }));
    expect(await screen.findByText(/Bu adım geri alınamaz/)).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Vazgeç' }));
    expect(screen.getByText('Yüklendi')).toBeInTheDocument();
    expect(screen.queryByText('Yolda')).not.toBeInTheDocument();
  });

  it('geçersiz sevkiyat kimliği için "bulunamadı" gösterir', async () => {
    server.use(
      ...createHandlers('DRIVER'),
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

  it('teslim karşılaştırması bitmeden itiraz isteği atmaz', async () => {
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
