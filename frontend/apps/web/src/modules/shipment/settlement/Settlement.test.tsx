import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { addMockReceipt } from '@/mocks/settlements';
import { renderApp } from '@/test/renderApp';

const COMPLETED = '/panel/sevkiyat/shp-0003';

async function openTab(name: string) {
  await userEvent.click(await screen.findByRole('tab', { name }));
}

function pick<T>(items: readonly T[], index: number): T {
  const item = items[index];
  expect(item).toBeDefined();
  return item as T;
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
    expect(
      within(form).getByText(/1\.900 kg Süprem kumaş kaydedilecek\. Kayıt sonradan/),
    ).toBeVisible();
    await userEvent.click(within(form).getByRole('button', { name: 'Evet, kaydet' }));
    expect(await within(form).findByText('Kumaş girişi kaydedildi.')).toBeInTheDocument();
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
    await userEvent.click(within(form).getByRole('combobox', { name: 'İplik cinsi' }));
    await userEvent.click(await screen.findByRole('option', { name: '36/1 Viskon' }));
    await userEvent.type(within(form).getByLabelText('Kilo'), '60');
    await userEvent.click(within(form).getByRole('button', { name: 'Kumaş girişini kaydet' }));
    expect(await within(form).findByText(/toplamı kumaş kilosuna eşit/)).toBeInTheDocument();
  });

  it('kumaş formu gönderilen ve gelen kiloyu yazar, kalanı aşan kiloyu göndermez', async () => {
    server.use(...createHandlers('QUALITY'));
    renderApp(COMPLETED);
    const form = await recordFabric('2.500');
    expect(await within(form).findByTestId('fabric-limit-note')).toHaveTextContent(
      'Fasoncuya 1.982,5 kg iplik gönderildi, şimdiye kadar 0 kg kumaş geldi; en çok 1.982,5 kg daha girebilirsiniz.',
    );
    expect(
      within(form).getByText(/ipliğin kalanından fazla kumaş yazamazsınız/),
    ).toBeInTheDocument();
    await userEvent.click(within(form).getByRole('button', { name: 'Kumaş girişini kaydet' }));
    expect(within(form).queryByRole('button', { name: 'Evet, kaydet' })).not.toBeInTheDocument();
  });

  it('iplik satırında yalnız sevkiyattaki iplikler sunulur, seçilen bir daha sunulmaz', async () => {
    server.use(...createHandlers('QUALITY'));
    renderApp(COMPLETED);
    await openTab('Kumaş girişi');
    const form = await screen.findByRole('region', { name: 'Kumaş girişi' });
    await userEvent.click(within(form).getByRole('button', { name: 'İplik satırı ekle' }));
    await userEvent.click(within(form).getByRole('button', { name: 'İplik satırı ekle' }));
    const combos = within(form).getAllByRole('combobox', { name: 'İplik cinsi' });
    const [first, second] = [pick(combos, 0), pick(combos, 1)];
    await userEvent.click(first);
    expect(await screen.findByRole('option', { name: '20 denye likra' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('option', { name: '36/1 Viskon' }));
    await userEvent.click(second);
    expect(await screen.findByRole('option', { name: '20 denye likra' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: '36/1 Viskon' })).not.toBeInTheDocument();
  });

  it('müdür iplik dönüşü kaydedemez; sekmede form görünmez ve Yön seçimi yoktur', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp(COMPLETED);
    await openTab('İplik dönüşü');
    expect(
      screen.queryByRole('region', { name: 'İplik dönüşü (fasoncudan depoya)' }),
    ).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Hareketi kaydet' })).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Yön')).not.toBeInTheDocument();
  });

  it('müdür kumaş girişi yapamaz; kumaş sekmesinde form görünmez', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp(COMPLETED);
    await openTab('Kumaş girişi');
    expect(screen.queryByRole('region', { name: 'Kumaş girişi' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Kumaş girişini kaydet' })).not.toBeInTheDocument();
  });

  it('müdür oranı değiştirir; faturalanacak kilo için fatura yerine bilgi görür', async () => {
    addMockReceipt(
      'shp-0003',
      { fabricType: 'Süprem', netKg: '1900', waybillNo: 'KUM-1' },
      'user-quality',
    );
    server.use(...createHandlers('MANAGER'));
    renderApp(COMPLETED);
    await openTab('Fire dosyası');
    await userEvent.type(await screen.findByLabelText('Değişiklik gerekçesi'), 'Sözleşme');
    await userEvent.click(screen.getByRole('button', { name: 'Oranı kaydet' }));
    const info = await screen.findByTestId('invoice-info');
    expect(info).toHaveTextContent(/faturalanacak\. Faturalama ekranından takip edebilirsiniz\./);
    expect(within(info).getByRole('link', { name: 'Faturalama ekranına git' })).toHaveAttribute(
      'href',
      '/panel/faturalama',
    );
    expect(screen.queryByLabelText(/Fatura no/)).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Fatura edildi' })).not.toBeInTheDocument();
  });

  it('müdür hatalı kilosu olmayan dosyayı kapatır', async () => {
    addMockReceipt(
      'shp-0003',
      { fabricType: 'Süprem', netKg: '2000', waybillNo: 'KUM-2' },
      'user-quality',
    );
    server.use(...createHandlers('MANAGER'));
    renderApp(COMPLETED);
    await openTab('Fire dosyası');
    await screen.findByTestId('fire-status');
    await userEvent.click(await screen.findByRole('button', { name: 'Kapat' }));
    expect(await screen.findByText('Dosya kapandı.')).toBeInTheDocument();
  });

  it('depo iplik dönüşü kaydeder ama kumaş ve fire sekmesini görmez', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp(COMPLETED);
    await openTab('İplik dönüşü');
    const form = await screen.findByRole('region', { name: 'İplik dönüşü (fasoncudan depoya)' });
    // İplik cinsi serbest yazılmaz; sevkiyatta gönderilen iplikler arasından seçilir.
    await userEvent.click(await within(form).findByRole('combobox', { name: /İplik cinsi/ }));
    expect(screen.queryByRole('option', { name: '30/1 Penye' })).not.toBeInTheDocument();
    await userEvent.click(await screen.findByRole('option', { name: '36/1 Viskon' }));
    await userEvent.type(within(form).getByLabelText(/Net kilo/), '25');
    await userEvent.click(within(form).getByRole('button', { name: 'Hareketi kaydet' }));
    expect(await screen.findByText(/Dönüş · 36\/1 Viskon · 25 kg/)).toBeInTheDocument();
    expect(screen.queryByRole('tab', { name: 'Kumaş girişi' })).not.toBeInTheDocument();
    expect(screen.queryByRole('tab', { name: 'Fire dosyası' })).not.toBeInTheDocument();
  });

  it('tamamlanmamış sevkiyatta fasoncu sekmeleri görünmez', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat/shp-0001');
    await screen.findByRole('tab', { name: 'Kilolar' });
    expect(screen.queryByRole('tab', { name: 'Fire dosyası' })).not.toBeInTheDocument();
  });
});
