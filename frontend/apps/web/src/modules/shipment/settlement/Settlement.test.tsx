import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { addMockReceipt, saveMockRecipe } from '@/mocks/settlements';
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

async function writeRatios(form: HTMLElement, viskon: string, likra: string) {
  await userEvent.type(await within(form).findByLabelText('Oran: 36/1 Viskon'), viskon);
  await userEvent.type(within(form).getByLabelText('Oran: 20 denye likra'), likra);
}

describe('Sevkiyat dosyası: fasoncu sekmeleri', () => {
  it('kalite iplik oranlarını yazar, önizlemeyi görür, kaydeder; liste ve fire dosyası güncellenir', async () => {
    server.use(...createHandlers('QUALITY'));
    renderApp(COMPLETED);
    const form = await recordFabric('1.900');
    await writeRatios(form, '78', '22');
    expect(within(form).getByTestId('fabric-ratio-total')).toHaveTextContent('Toplam: %100');
    await userEvent.click(within(form).getByRole('button', { name: 'Kontrol et' }));
    expect(await within(form).findByText('Kumaş için şu iplikler düşülecek')).toBeVisible();
    expect(within(form).getAllByText(/sevkiyatından (1\.482|418) kg/)).toHaveLength(2);
    expect(
      within(form).getByText(/1\.900 kg Süprem kumaş kaydedilecek\. Kayıt sonradan/),
    ).toBeVisible();
    await userEvent.click(within(form).getByRole('button', { name: 'Evet, kaydet' }));
    expect(await within(form).findByText('Kumaş girişi kaydedildi.')).toBeInTheDocument();
    expect(await screen.findByText(/Süprem · 1\.900 kg/)).toBeInTheDocument();
    await openTab('Fire dosyası');
    expect(await screen.findByTestId('fire-status')).toHaveTextContent('Faturalanacak');
    expect(screen.getByText('3,2 kg')).toBeInTheDocument();
    expect(await screen.findByTestId('fabric-draws')).toHaveTextContent(/36\/1 Viskon → Süprem/);
  });

  it('oranların toplamı 100 değilse canlı toplam kırmızı kalır ve kontrol uyarır', async () => {
    server.use(...createHandlers('QUALITY'));
    renderApp(COMPLETED);
    const form = await recordFabric('100');
    await writeRatios(form, '60', '30');
    expect(within(form).getByTestId('fabric-ratio-total')).toHaveTextContent(
      'Toplam: %90 (tam %100 olmalı)',
    );
    await userEvent.click(within(form).getByRole('button', { name: 'Kontrol et' }));
    expect(await within(form).findByText('Oranların toplamı tam %100 olmalı.')).toBeInTheDocument();
    expect(within(form).queryByRole('button', { name: 'Evet, kaydet' })).not.toBeInTheDocument();
  });

  it('fasoncuda yetmeyen iplik için eksik kiloyu yazar ve kaydettirmez', async () => {
    server.use(...createHandlers('QUALITY'));
    renderApp(COMPLETED);
    // %40 likra = 800 kg; fasoncuda yalnız 482,5 kg likra var
    const form = await recordFabric('2.000');
    await writeRatios(form, '60', '40');
    await userEvent.click(within(form).getByRole('button', { name: 'Kontrol et' }));
    expect(await within(form).findByText('Fasoncuda yeterli iplik yok')).toBeVisible();
    expect(within(form).getByText(/20 denye likra: 317,5 kg eksik/)).toBeInTheDocument();
    expect(within(form).queryByRole('button', { name: 'Evet, kaydet' })).not.toBeInTheDocument();
  });

  it('tanımlı karışım kumaş cinsine göre oranları doldurur; kalite düzeltebilir', async () => {
    saveMockRecipe(undefined, {
      fabricType: 'süprem',
      yarns: [
        { yarnType: '36/1 Viskon', ratioPct: '70' },
        { yarnType: '20 denye likra', ratioPct: '30' },
      ],
    });
    server.use(...createHandlers('QUALITY'));
    renderApp(COMPLETED);
    const form = await recordFabric('100');
    await userEvent.click(
      await within(form).findByRole('button', { name: 'Tanımlı karışımı yaz' }),
    );
    expect(within(form).getByLabelText('Oran: 36/1 Viskon')).toHaveValue('70');
    expect(within(form).getByLabelText('Oran: 20 denye likra')).toHaveValue('30');
    expect(within(form).getByTestId('fabric-ratio-total')).toHaveTextContent('Toplam: %100');
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
