import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

const OPEN_DISCREPANCY = {
  id: 'disc-x',
  discrepancyNo: 'UY-2026-0042',
  siteId: 'site-0',
  shipmentId: 'shp-0001',
  type: 'QUALITY',
  severity: 'RED',
  status: 'OPEN',
  ruleCode: 'QUALITY_CLAIM',
  createdAt: '2026-11-20T08:00:00Z',
};

describe('Fasoncu sayfası', () => {
  it('bakiye tablosundaki fasoncu adı fasoncu sayfasına götürür', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/bakiye');
    const table = await screen.findByRole('table', { name: 'Fasoncu bakiyesi' });
    expect(within(table).getByRole('link', { name: 'Aydın Örme' })).toHaveAttribute(
      'href',
      '/panel/bakiye/fasoncu/sub-0',
    );
  });

  it('fasoncunun bakiyesini, yoldaki sevkiyatlarını ve açık uyuşmazlığını tek ekranda gösterir', async () => {
    server.use(...createHandlers('MANAGER'));
    server.use(
      http.get('/api/v1/discrepancies', ({ request }) =>
        HttpResponse.json({
          items:
            new URL(request.url).searchParams.get('status') === 'OPEN' ? [OPEN_DISCREPANCY] : [],
          page: { number: 0, size: 100, totalElements: 1, totalPages: 1 },
        }),
      ),
    );
    renderApp('/panel/bakiye/fasoncu/sub-0');
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Aydın Örme' }),
    ).toBeInTheDocument();
    expect(await screen.findByRole('link', { name: 'UY-2026-0042' })).toHaveAttribute(
      'href',
      '/panel/uyusmazliklar/disc-x',
    );
    expect(screen.getByTestId('sub-open-discrepancies')).toHaveTextContent('1');
    expect(screen.getByTestId('sub-balance')).toHaveTextContent('kg');
    expect(screen.getByRole('link', { name: 'Fasoncu bakiyesine dön' })).toHaveAttribute(
      'href',
      '/panel/bakiye',
    );
    const table = await screen.findByRole('table', { name: 'Fasoncu bakiyesi' });
    expect(within(table).queryByRole('link')).not.toBeInTheDocument();
  });

  it('bakiye yetkisi olmayan rol sayfayı göremez', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp('/panel/bakiye/fasoncu/sub-0');
    expect(await screen.findByText('Bu sayfayı görme yetkiniz yok.')).toBeInTheDocument();
  });
});
