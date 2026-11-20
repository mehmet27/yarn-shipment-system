import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

const EMPTY_PAGE = { items: [], page: { number: 0, size: 20, totalElements: 0, totalPages: 0 } };

describe('Ana sayfa: sizi bekleyen işler', () => {
  it('müdür irsaliyesi ve faturası beklenen işleri görür', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel');
    const tasks = within(await screen.findByTestId('home-tasks'));
    expect(tasks.getByRole('heading', { name: 'Sizi bekleyen işler' })).toBeInTheDocument();
    const deliveries = within(await tasks.findByTestId('pending-deliveries'));
    expect(deliveries.getByText(/İrsaliyesi beklenen sevkiyat: \d+/)).toBeInTheDocument();
    expect(deliveries.getByRole('link', { name: 'SVK-2026-0002' })).toHaveAttribute(
      'href',
      '/panel/sevkiyat/shp-0002',
    );
    expect(tasks.queryByTestId('pending-dispatches')).not.toBeInTheDocument();
    expect(tasks.queryByTestId('pending-fabric')).not.toBeInTheDocument();
  });

  it('kalite kumaşı beklenen sevkiyatları görür; satır kumaş girişi sekmesini açar', async () => {
    server.use(...createHandlers('QUALITY'));
    renderApp('/panel');
    const fabric = within(await screen.findByTestId('pending-fabric'));
    expect(fabric.getByText(/Kumaşı beklenen sevkiyat: \d+/)).toBeInTheDocument();
    expect(fabric.getByRole('link', { name: 'SVK-2026-0003' })).toHaveAttribute(
      'href',
      '/panel/sevkiyat/shp-0003?sekme=kumas',
    );
  });

  it('depo yüklenecek sevkiyatları görür; müdürün ve kalitenin işleri görünmez', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel');
    const tasks = within(await screen.findByTestId('home-tasks'));
    expect(await tasks.findByTestId('pending-dispatches')).toBeInTheDocument();
    expect(tasks.queryByTestId('pending-deliveries')).not.toBeInTheDocument();
    expect(tasks.queryByTestId('pending-invoices')).not.toBeInTheDocument();
    expect(tasks.queryByTestId('pending-fabric')).not.toBeInTheDocument();
  });

  it('bekleyen iş yoksa depoya bunu ve işin nereden geleceğini söyler', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    server.use(http.get('/api/v1/shipments', () => HttpResponse.json(EMPTY_PAGE)));
    renderApp('/panel');
    expect(await screen.findByText('Şu an sizi bekleyen iş yok')).toBeInTheDocument();
    expect(screen.getByText(/Müdür yeni sevkiyat açınca burada/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Sevkiyatlara git' })).toHaveAttribute(
      'href',
      '/panel/sevkiyat',
    );
  });

  it('yöneticide iş listesi bölümü yoktur', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp('/panel');
    await screen.findByText(/Hoş geldiniz/);
    expect(screen.queryByTestId('home-tasks')).not.toBeInTheDocument();
  });
});
