import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

const photo = () => new File(['x'.repeat(2000)], 'leke.jpg', { type: 'image/jpeg' });
const report = () => new File(['%PDF'], 'rapor.pdf', { type: 'application/pdf' });

describe('Kalite itirazı', () => {
  it('fasoncu teslim edilen çuvala fotoğraflı itiraz açar', async () => {
    server.use(...createHandlers('SUBCONTRACTOR'));
    renderApp('/saha/sevkiyat/shp-0007');
    await userEvent.click(await screen.findByRole('button', { name: 'Kalite itirazı aç' }));
    const form = screen.getByRole('region', { name: 'Yeni kalite itirazı' });
    const submit = within(form).getByRole('button', { name: 'İtirazı gönder' });
    await userEvent.click(within(form).getByRole('checkbox', { name: 'BAG-0002' }));
    await userEvent.type(within(form).getByLabelText(/Sorunu anlatın/), 'Çuvalda leke var');
    expect(submit).toBeDisabled();
    await userEvent.upload(within(form).getByTestId('upload-file-input'), photo());
    expect(await within(form).findByText('1 fotoğraf eklendi.')).toBeInTheDocument();
    await userEvent.click(submit);
    const card = await screen.findByRole('article', { name: 'UY-shp-0007-1' });
    expect(within(card).getByText('Çuvallar: BAG-0002')).toBeInTheDocument();
    expect(screen.queryByRole('region', { name: 'Yeni kalite itirazı' })).not.toBeInTheDocument();
  });

  it('süre dolduysa sunucu hatasını Türkçe gösterir', async () => {
    server.use(
      http.post('/api/v1/quality-claims', () =>
        HttpResponse.json(
          { status: 409, code: 'QUALITY_CLAIM_WINDOW_CLOSED', title: 'x' },
          { status: 409 },
        ),
      ),
      ...createHandlers('SUBCONTRACTOR'),
    );
    renderApp('/saha/sevkiyat/shp-0007');
    await userEvent.click(await screen.findByRole('button', { name: 'Kalite itirazı aç' }));
    const form = screen.getByRole('region', { name: 'Yeni kalite itirazı' });
    await userEvent.click(within(form).getByRole('checkbox', { name: 'BAG-0001' }));
    await userEvent.type(within(form).getByLabelText(/Sorunu anlatın/), 'Nem');
    await userEvent.upload(within(form).getByTestId('upload-file-input'), photo());
    await within(form).findByText('1 fotoğraf eklendi.');
    await userEvent.click(within(form).getByRole('button', { name: 'İtirazı gönder' }));
    expect(await within(form).findByText(/İtiraz süresi doldu/)).toBeInTheDocument();
  });

  it('teslim edilmemiş sevkiyatta itiraz bölümü görünmez', async () => {
    server.use(...createHandlers('SUBCONTRACTOR'));
    renderApp('/saha/sevkiyat/shp-0004');
    await screen.findByText('Teslim bekliyor');
    expect(screen.queryByText('Kalite itirazı')).not.toBeInTheDocument();
  });

  it('kalite lab analizi ister, raporla sonucu kaydeder', async () => {
    server.use(...createHandlers('QUALITY'));
    renderApp('/panel/sevkiyat/shp-0013');
    await userEvent.click(await screen.findByRole('tab', { name: 'Kalite itirazı' }));
    const card = await screen.findByRole('article', { name: 'UY-shp-0013-1' });
    await userEvent.click(within(card).getByText('Laboratuvar analizi'));
    await userEvent.type(within(card).getByLabelText('Talep notu'), 'Şahit numuneyle karşılaştır');
    await userEvent.click(within(card).getByRole('button', { name: 'Kontrol iste' }));
    expect(await within(card).findByText('Sonuç bekleniyor')).toBeInTheDocument();
    const save = within(card).getByRole('button', { name: 'Sonucu kaydet' });
    await userEvent.type(within(card).getByLabelText(/^Sonuç/), 'Nem oranı yüksek');
    expect(save).toBeDisabled();
    await userEvent.upload(within(card).getByTestId('upload-file-input'), report());
    await userEvent.click(await within(card).findByRole('button', { name: 'Sonucu kaydet' }));
    expect(await within(card).findByText('Sonuçlandı')).toBeInTheDocument();
    expect(within(card).getByText('Rapor belgesi eklendi.')).toBeInTheDocument();
    expect(within(card).queryByRole('button', { name: 'Sonucu kaydet' })).not.toBeInTheDocument();
  });

  it('yönetici itirazları görür ama kontrol isteyemez', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat/shp-0013');
    await userEvent.click(await screen.findByRole('tab', { name: 'Kalite itirazı' }));
    expect(await screen.findByRole('article', { name: 'UY-shp-0013-1' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Kontrol iste' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Kalite itirazı aç' })).not.toBeInTheDocument();
  });

  it('kalite uyuşmazlığının detayında itiraz dosyasını gösterir', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp('/panel/uyusmazliklar/disc-3');
    expect(await screen.findByText('İtiraz dosyası')).toBeInTheDocument();
    expect(screen.getByText('Çuvalda leke ve nem var.')).toBeInTheDocument();
  });
});
