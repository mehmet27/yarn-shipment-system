import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import type { RequestHandler } from 'msw';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

const photo = () => new File(['x'.repeat(2000)], 'leke.jpg', { type: 'image/jpeg' });

describe('Kalite itirazı', () => {
  async function openForm(role: 'QUALITY', ...extra: RequestHandler[]) {
    server.use(...extra, ...createHandlers(role));
    renderApp('/panel/sevkiyat/shp-0019');
    await userEvent.click(await screen.findByRole('tab', { name: 'Kalite itirazı' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Kalite itirazı aç' }));
    return screen.getByRole('region', { name: 'Yeni kalite itirazı' });
  }

  it.each(['QUALITY'] as const)(
    '%s tamamlanmış sevkiyata kg ile fotoğraflı itiraz açar',
    async (role) => {
      const form = await openForm(role);
      const submit = within(form).getByRole('button', { name: 'İtirazı gönder' });
      expect(within(form).queryByRole('checkbox')).not.toBeInTheDocument();
      await userEvent.type(within(form).getByLabelText(/İtiraz edilen kilo/), '12,5');
      await userEvent.type(within(form).getByLabelText(/Sorunu anlatın/), 'Kumaşta leke var');
      expect(submit).toBeDisabled();
      await userEvent.upload(within(form).getByTestId('upload-file-input'), photo());
      expect(await within(form).findByText('1 fotoğraf eklendi.')).toBeInTheDocument();
      await userEvent.click(submit);
      const cards = await screen.findAllByRole('article');
      const card = cards.find((item) => within(item).queryByText('İtiraz edilen kilo: 12,5 kg'));
      expect(card).toBeDefined();
      expect(screen.queryByRole('region', { name: 'Yeni kalite itirazı' })).not.toBeInTheDocument();
    },
  );

  it('geçersiz kiloda gönder düğmesi kapalı kalır', async () => {
    const form = await openForm('QUALITY');
    await userEvent.type(within(form).getByLabelText(/İtiraz edilen kilo/), '0');
    await userEvent.type(within(form).getByLabelText(/Sorunu anlatın/), 'Nem');
    await userEvent.upload(within(form).getByTestId('upload-file-input'), photo());
    await within(form).findByText('1 fotoğraf eklendi.');
    expect(within(form).getByRole('button', { name: 'İtirazı gönder' })).toBeDisabled();
    expect(within(form).getByText(/0'dan büyük/)).toBeInTheDocument();
  });

  it('ipucu gerçek sınırı yazar; aşan kiloda alan hatası çıkar ve gönder kapalı kalır', async () => {
    const form = await openForm('QUALITY');
    expect(
      within(form).getByText('Girilen kumaşa göre en çok 1.862,5 kg yazabilirsiniz.'),
    ).toBeInTheDocument();
    await userEvent.type(within(form).getByLabelText(/İtiraz edilen kilo/), '1.862,6');
    await userEvent.type(within(form).getByLabelText(/Sorunu anlatın/), 'Nem');
    await userEvent.upload(within(form).getByTestId('upload-file-input'), photo());
    await within(form).findByText('1 fotoğraf eklendi.');
    expect(within(form).getByText(/İtiraz edilebilecek kilo 1\.862,5 kg/)).toBeInTheDocument();
    expect(within(form).getByRole('button', { name: 'İtirazı gönder' })).toBeDisabled();
  });

  it('kumaş girişi yoksa itiraz formu açılmaz ve nedeni yazılır', async () => {
    const noFabric = http.get('/api/v1/quality-claims/shipments/:shipmentId', () =>
      HttpResponse.json({
        shipmentId: 'shp-0019',
        deadlineAt: '2026-10-29T09:00:00Z',
        windowOpen: true,
        receivedKg: '0.000',
        claimableKg: '0.000',
        claims: [],
        allowedActions: ['OPEN_CLAIM'],
      }),
    );
    server.use(noFabric, ...createHandlers('QUALITY'));
    renderApp('/panel/sevkiyat/shp-0019');
    await userEvent.click(await screen.findByRole('tab', { name: 'Kalite itirazı' }));
    expect(await screen.findByText(/önce kumaş girişi yapılmalı/)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Kalite itirazı aç' })).not.toBeInTheDocument();
  });

  it('girilen kumaşın tamamına itiraz açıldıysa yeni itiraz açılmaz', async () => {
    const allClaimed = http.get('/api/v1/quality-claims/shipments/:shipmentId', () =>
      HttpResponse.json({
        shipmentId: 'shp-0019',
        deadlineAt: '2026-10-29T09:00:00Z',
        windowOpen: true,
        receivedKg: '100.000',
        claimableKg: '0.000',
        claims: [],
        allowedActions: ['OPEN_CLAIM'],
      }),
    );
    server.use(allClaimed, ...createHandlers('QUALITY'));
    renderApp('/panel/sevkiyat/shp-0019');
    await userEvent.click(await screen.findByRole('tab', { name: 'Kalite itirazı' }));
    expect(await screen.findByText(/tamamı için itiraz açıldı/)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Kalite itirazı aç' })).not.toBeInTheDocument();
  });

  it('sunucu kilo aşımını reddederse hatasını Türkçe gösterir', async () => {
    const reject = http.post('/api/v1/quality-claims', () =>
      HttpResponse.json(
        { status: 409, code: 'CLAIM_EXCEEDS_SHIPMENT_WEIGHT', title: 'x' },
        { status: 409 },
      ),
    );
    const form = await openForm('QUALITY', reject);
    await userEvent.type(within(form).getByLabelText(/İtiraz edilen kilo/), '10');
    await userEvent.type(within(form).getByLabelText(/Sorunu anlatın/), 'Nem');
    await userEvent.upload(within(form).getByTestId('upload-file-input'), photo());
    await within(form).findByText('1 fotoğraf eklendi.');
    await userEvent.click(within(form).getByRole('button', { name: 'İtirazı gönder' }));
    expect(await within(form).findByText(/itiraz edilebilecek kiloyu aşıyor/)).toBeInTheDocument();
  });

  it('süre dolduysa sunucu hatasını Türkçe gösterir', async () => {
    const closed = http.post('/api/v1/quality-claims', () =>
      HttpResponse.json(
        { status: 409, code: 'QUALITY_CLAIM_WINDOW_CLOSED', title: 'x' },
        { status: 409 },
      ),
    );
    const form = await openForm('QUALITY', closed);
    await userEvent.type(within(form).getByLabelText(/İtiraz edilen kilo/), '10');
    await userEvent.type(within(form).getByLabelText(/Sorunu anlatın/), 'Nem');
    await userEvent.upload(within(form).getByTestId('upload-file-input'), photo());
    await within(form).findByText('1 fotoğraf eklendi.');
    await userEvent.click(within(form).getByRole('button', { name: 'İtirazı gönder' }));
    expect(await within(form).findByText(/şu an itiraz açılamıyor/)).toBeInTheDocument();
  });

  it('tamamlanmamış sevkiyatta itiraz sekmesi görünmez', async () => {
    server.use(...createHandlers('QUALITY'));
    renderApp('/panel/sevkiyat/shp-0010');
    await screen.findByTestId('shipment-status');
    expect(screen.queryByRole('tab', { name: 'Kalite itirazı' })).not.toBeInTheDocument();
  });

  it('müdür itirazları görür ama itiraz açamaz', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat/shp-0019');
    await userEvent.click(await screen.findByRole('tab', { name: 'Kalite itirazı' }));
    expect(await screen.findByRole('article', { name: 'UY-shp-0019-1' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Kalite itirazı aç' })).not.toBeInTheDocument();
  });

  it('kalite uyuşmazlığının detayında itiraz dosyasını gösterir', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/uyusmazliklar/disc-3');
    expect(await screen.findByText('İtiraz dosyası')).toBeInTheDocument();
    expect(screen.getByText('Kumaşta leke ve nem var.')).toBeInTheDocument();
  });
});
