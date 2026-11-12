import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import type { RequestHandler } from 'msw';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

const photo = () => new File(['x'.repeat(2000)], 'leke.jpg', { type: 'image/jpeg' });
const report = () => new File(['%PDF'], 'rapor.pdf', { type: 'application/pdf' });

describe('Kalite itirazı', () => {
  async function openForm(role: 'QUALITY' | 'MANAGER' | 'ADMIN', ...extra: RequestHandler[]) {
    server.use(...extra, ...createHandlers(role));
    renderApp('/panel/sevkiyat/shp-0019');
    await userEvent.click(await screen.findByRole('tab', { name: 'Kalite itirazı' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Kalite itirazı aç' }));
    return screen.getByRole('region', { name: 'Yeni kalite itirazı' });
  }

  it.each(['QUALITY', 'MANAGER', 'ADMIN'] as const)(
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

  it('kilo sevkiyat ağırlığını aşarsa sunucu hatasını Türkçe gösterir', async () => {
    const form = await openForm('QUALITY');
    await userEvent.type(within(form).getByLabelText(/İtiraz edilen kilo/), '99999');
    await userEvent.type(within(form).getByLabelText(/Sorunu anlatın/), 'Nem');
    await userEvent.upload(within(form).getByTestId('upload-file-input'), photo());
    await within(form).findByText('1 fotoğraf eklendi.');
    await userEvent.click(within(form).getByRole('button', { name: 'İtirazı gönder' }));
    expect(await within(form).findByText(/net ağırlığını aşıyor/)).toBeInTheDocument();
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
    expect(await within(form).findByText(/İtiraz süresi doldu/)).toBeInTheDocument();
  });

  it('tamamlanmamış sevkiyatta itiraz sekmesi görünmez', async () => {
    server.use(...createHandlers('QUALITY'));
    renderApp('/panel/sevkiyat/shp-0010');
    await screen.findByTestId('shipment-status');
    expect(screen.queryByRole('tab', { name: 'Kalite itirazı' })).not.toBeInTheDocument();
  });

  it('kalite lab analizi ister, raporla sonucu kaydeder', async () => {
    server.use(...createHandlers('QUALITY'));
    renderApp('/panel/sevkiyat/shp-0019');
    await userEvent.click(await screen.findByRole('tab', { name: 'Kalite itirazı' }));
    const card = await screen.findByRole('article', { name: 'UY-shp-0019-1' });
    await userEvent.type(within(card).getByLabelText('Talep notu'), 'Nem analizi iste');
    await userEvent.click(within(card).getByRole('button', { name: 'Kontrol iste' }));
    expect(await within(card).findByText('Sonuç bekleniyor')).toBeInTheDocument();
    const save = within(card).getByRole('button', { name: 'Sonucu kaydet' });
    await userEvent.type(within(card).getByLabelText(/^Sonuç/), 'Nem oranı yüksek');
    expect(save).toBeDisabled();
    await userEvent.upload(within(card).getByTestId('upload-file-input'), report());
    await userEvent.click(await within(card).findByRole('button', { name: 'Sonucu kaydet' }));
    expect(await within(card).findByText('Sonuçlandı')).toBeInTheDocument();
    expect(await within(card).findByLabelText('Laboratuvar sonucu')).toBeInTheDocument();
    expect(within(card).queryByRole('button', { name: 'Sonucu kaydet' })).not.toBeInTheDocument();
  });

  it('müdür itirazları görür ama kontrol isteyemez', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat/shp-0019');
    await userEvent.click(await screen.findByRole('tab', { name: 'Kalite itirazı' }));
    expect(await screen.findByRole('article', { name: 'UY-shp-0019-1' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Kontrol iste' })).not.toBeInTheDocument();
  });

  it('kalite uyuşmazlığının detayında itiraz dosyasını gösterir', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp('/panel/uyusmazliklar/disc-3');
    expect(await screen.findByText('İtiraz dosyası')).toBeInTheDocument();
    expect(screen.getByText('Kumaşta leke ve nem var.')).toBeInTheDocument();
  });
});
