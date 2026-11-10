import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

const COMPLETED = '/panel/sevkiyat/shp-0003';

async function openTab(name: string) {
  await userEvent.click(await screen.findByRole('tab', { name }));
}

async function recordFabric(kg: string) {
  await openTab('Kumaş girişi');
  const form = await screen.findByRole('region', { name: 'Kumaş girişi' });
  await userEvent.type(within(form).getByLabelText(/Kumaş cinsi/), 'Süprem');
  await userEvent.type(within(form).getByLabelText(/Kumaş net kilosu/), kg);
  await userEvent.type(within(form).getByLabelText(/İrsaliye no/), 'KUM-1');
  return form;
}

describe('Sevkiyat dosyası: fasoncu sekmeleri', () => {
  it('kalite kumaş girişi kaydeder, liste ve fire dosyası güncellenir', async () => {
    server.use(...createHandlers('QUALITY'));
    renderApp(COMPLETED);
    const form = await recordFabric('1.900');
    await userEvent.click(within(form).getByRole('button', { name: 'Kumaş girişini kaydet' }));
    expect(await screen.findByText(/Süprem · 1\.900 kg/)).toBeInTheDocument();
    await openTab('Fire dosyası');
    expect(await screen.findByTestId('fire-status')).toHaveTextContent('Faturalanacak');
    expect(screen.getByText('3,2 kg')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Fatura edildi' })).not.toBeInTheDocument();
  });

  it('iplik dağılımı toplamı kumaş kilosuna eşit değilse uyarır', async () => {
    server.use(...createHandlers('QUALITY'));
    renderApp(COMPLETED);
    const form = await recordFabric('100');
    await userEvent.click(within(form).getByRole('button', { name: 'İplik satırı ekle' }));
    await userEvent.type(within(form).getByLabelText('İplik cinsi'), 'Penye');
    await userEvent.type(within(form).getByLabelText('Kilo'), '60');
    await userEvent.click(within(form).getByRole('button', { name: 'Kumaş girişini kaydet' }));
    expect(await within(form).findByText(/toplamı kumaş kilosuna eşit/)).toBeInTheDocument();
  });

  it('müdür oranı değiştirir ve hatalı kiloyu faturalar; dosya kapanır', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp(COMPLETED);
    const form = await recordFabric('1.900');
    await userEvent.click(within(form).getByRole('button', { name: 'Kumaş girişini kaydet' }));
    await screen.findByText(/Süprem · 1\.900 kg/);
    await openTab('Fire dosyası');
    await userEvent.type(await screen.findByLabelText('Değişiklik gerekçesi'), 'Sözleşme');
    await userEvent.click(screen.getByRole('button', { name: 'Oranı kaydet' }));
    await userEvent.type(await screen.findByLabelText(/Fatura no/), 'FTR-7');
    await userEvent.click(screen.getByRole('button', { name: 'Fatura edildi' }));
    expect(await screen.findByText(/Fatura FTR-7/)).toBeInTheDocument();
    expect(screen.getByTestId('fire-status')).toHaveTextContent('Faturalandı');
    expect(screen.queryByRole('button', { name: 'Oranı kaydet' })).not.toBeInTheDocument();
  });

  it('müdür hatalı kilosu olmayan dosyayı kapatır', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp(COMPLETED);
    const form = await recordFabric('2.000');
    await userEvent.click(within(form).getByRole('button', { name: 'Kumaş girişini kaydet' }));
    await screen.findByText(/Süprem · 2\.000 kg/);
    await openTab('Fire dosyası');
    await userEvent.click(await screen.findByRole('button', { name: 'Kapat' }));
    expect(await screen.findByText('Dosya kapandı.')).toBeInTheDocument();
  });

  it('depo iplik dönüşü kaydeder ama kumaş ve fire sekmesini görmez', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp(COMPLETED);
    await openTab('İplik dönüşü');
    const form = await screen.findByRole('region', { name: 'İplik dönüşü / gelen iplik' });
    await userEvent.type(within(form).getByLabelText(/İplik cinsi/), '30/1 Penye');
    await userEvent.type(within(form).getByLabelText(/Net kilo/), '25');
    await userEvent.click(within(form).getByRole('button', { name: 'Hareketi kaydet' }));
    expect(await screen.findByText(/Dönüş · 30\/1 Penye · 25 kg/)).toBeInTheDocument();
    expect(screen.queryByRole('tab', { name: 'Kumaş girişi' })).not.toBeInTheDocument();
    expect(screen.queryByRole('tab', { name: 'Fire dosyası' })).not.toBeInTheDocument();
  });

  it('tamamlanmamış sevkiyatta fasoncu sekmeleri görünmez', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp('/panel/sevkiyat/shp-0001');
    await screen.findByRole('tab', { name: 'Kilolar' });
    expect(screen.queryByRole('tab', { name: 'Fire dosyası' })).not.toBeInTheDocument();
  });
});
