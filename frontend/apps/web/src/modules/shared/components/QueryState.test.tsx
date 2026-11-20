import { SHIPMENTS_PATH } from '@iplik/api-client';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

describe('QueryState', () => {
  it('yüklenirken ekran okuyucuya "Yükleniyor…" der', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat');
    expect(await screen.findByRole('status')).toBeInTheDocument();
    expect(screen.getByLabelText('Yükleniyor…')).toBeInTheDocument();
  });

  it('geçici hatada "Yeniden dene" sorguyu tekrarlar ve liste gelir', async () => {
    server.use(...createHandlers('MANAGER'));
    let isFailing = true;
    server.use(
      http.get(SHIPMENTS_PATH, () =>
        isFailing ? HttpResponse.json({ status: 500 }, { status: 500 }) : undefined,
      ),
    );
    renderApp('/panel/sevkiyat');
    expect(await screen.findByText(/Beklenmeyen bir hata oluştu/)).toBeInTheDocument();
    isFailing = false;
    await userEvent.click(screen.getByRole('button', { name: 'Yeniden dene' }));
    expect(await screen.findAllByText('SVK-2026-0001', {}, { timeout: 10_000 })).not.toHaveLength(
      0,
    );
  });

  it('bulunamayan kayıtta yeniden deneme düğmesi göstermez', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat/yok-boyle');
    expect(await screen.findByText('Aradığınız sayfa bulunamadı.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Yeniden dene' })).not.toBeInTheDocument();
  });
});
