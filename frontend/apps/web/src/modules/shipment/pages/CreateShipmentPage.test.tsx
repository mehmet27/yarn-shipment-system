import { todayIsoDate } from '@iplik/i18n';
import { act, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { saveMockRecipe } from '@/mocks/settlements';
import { renderApp } from '@/test/renderApp';

async function pick(label: string, option: string) {
  await userEvent.click(await screen.findByRole('combobox', { name: new RegExp(label) }));
  await userEvent.click(await screen.findByRole('option', { name: option }));
}

async function fillRecipe(netKg: string, withPrice = true) {
  await pick('İplik cinsi 1', 'Pamuk 20/1');
  await userEvent.type(screen.getByLabelText(/Net kg 1/), netKg);
  if (!withPrice) return;
  await userEvent.click(screen.getByRole('tab', { name: /Sipariş bilgileri/ }));
  await userEvent.type(screen.getByLabelText(/Birim fiyat/), '21,5');
}

const LONG_FLOW_TIMEOUT_MS = 20_000;

describe('CreateShipmentPage', () => {
  it(
    'müdür taslak sevkiyat oluşturur ve dosyasına gider',
    async () => {
      server.use(...createHandlers('MANAGER'));
      renderApp('/panel/sevkiyat');
      await userEvent.click(await screen.findByRole('link', { name: 'Yeni sevkiyat' }));
      await pick('Alıcı fasoncu', 'Bursa Tekstil');
      await pick('Şoför', 'Mert Kaya');
      await fillRecipe('2.036');
      expect(screen.getByLabelText(/Fire oranı/)).toHaveValue('4');
      expect(screen.getByTestId('recipe-total')).toHaveTextContent('2.036 kg');
      await userEvent.click(screen.getByRole('button', { name: 'Taslak oluştur' }));
      expect(await screen.findByText('SVK-2026-0046')).toBeInTheDocument();
      expect(screen.getByText('Taslak', { selector: '.mantine-Badge-label' })).toBeInTheDocument();
    },
    LONG_FLOW_TIMEOUT_MS,
  );

  it(
    'fiyat boşken kilo hatası olan satırda İplik sekmesi açık kalır',
    async () => {
      server.use(...createHandlers('MANAGER'));
      renderApp('/panel/sevkiyat/yeni');
      await pick('Alıcı fasoncu', 'Bursa Tekstil');
      await pick('İplik cinsi 1', 'Pamuk 20/1');
      await userEvent.click(screen.getByRole('button', { name: 'Taslak oluştur' }));
      expect(await screen.findByLabelText(/Net kg 1/)).toBeVisible();
    },
    LONG_FLOW_TIMEOUT_MS,
  );

  it(
    'art arda iki tıklama aynı sipariş için tek sevkiyat açar',
    async () => {
      server.use(...createHandlers('MANAGER'));
      let posts = 0;
      server.events.on('request:start', ({ request }) => {
        if (request.method === 'POST' && new URL(request.url).pathname === '/api/v1/shipments') {
          posts += 1;
        }
      });
      renderApp('/panel/sevkiyat/yeni');
      await pick('Alıcı fasoncu', 'Bursa Tekstil');
      await fillRecipe('2.036');
      const button = await screen.findByRole('button', { name: 'Taslak oluştur' });
      act(() => {
        button.click();
        button.click();
      });
      expect(await screen.findByText('SVK-2026-0046')).toBeInTheDocument();
      expect(posts).toBe(1);
      server.events.removeAllListeners();
    },
    LONG_FLOW_TIMEOUT_MS,
  );

  it(
    'sevkiyat tarihi bugünle gelir; silinirse gönderilmez ve tarih seçin uyarısı çıkar',
    async () => {
      server.use(...createHandlers('MANAGER'));
      renderApp('/panel/sevkiyat/yeni');
      const date = await screen.findByLabelText(/Sevkiyat tarihi/);
      expect(date).toHaveValue(todayIsoDate());
      await userEvent.clear(date);
      await pick('Alıcı fasoncu', 'Bursa Tekstil');
      await fillRecipe('2.036');
      await userEvent.click(screen.getByRole('button', { name: 'Taslak oluştur' }));
      expect(await screen.findByText('Sevkiyat tarihini seçin.')).toBeInTheDocument();
    },
    LONG_FLOW_TIMEOUT_MS,
  );

  it('kumaş cinsi tanımlı karışımdan seçilir; LYC elastandan dolar, karışımda olmayan iplik uyarılır', async () => {
    saveMockRecipe(undefined, {
      fabricType: 'Likralı Süprem',
      yarns: [
        { yarnType: 'Pamuk 20/1', ratioPct: '95.00' },
        { yarnType: '30 Denye Elastan', ratioPct: '5.00' },
      ],
    });
    server.use(
      http.get('/api/v1/master-data/products', ({ request }) => {
        const q = new URL(request.url).searchParams.get('q') ?? '';
        const all = [
          { id: 'prd-ea', code: 'EA-30D', name: '30 Denye Elastan', yarnCount: '30 denye' },
          { id: 'prd-0', code: 'PES-30-1', name: 'Polyester 30/1' },
        ];
        const items = all.filter((p) => p.name.includes(q)).map((p) => ({ ...p, active: true }));
        return HttpResponse.json({
          items,
          page: { number: 0, size: 100, totalElements: items.length, totalPages: 1 },
        });
      }),
      ...createHandlers('MANAGER'),
    );
    renderApp('/panel/sevkiyat/yeni');
    await pick('İplik cinsi 1', 'Polyester 30/1');
    await userEvent.click(screen.getByRole('tab', { name: /Sipariş bilgileri/ }));
    await userEvent.click(screen.getByRole('combobox', { name: 'Kumaş cinsi' }));
    await userEvent.click(await screen.findByRole('option', { name: 'Likralı Süprem' }));

    await waitFor(() => {
      expect(screen.getByLabelText(/^LYC/)).toHaveValue('30 denye');
    });
    expect(
      screen.getByText('Seçili iplik bu kumaşın tanımlı karışımında yok.'),
    ).toBeInTheDocument();

    await userEvent.clear(screen.getByRole('combobox', { name: 'Kumaş cinsi' }));
    expect(screen.getByLabelText(/^LYC/)).toHaveValue('');
  });

  it('fasoncu seçilmeden göndermeyi engeller', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat/yeni');
    await userEvent.click(await screen.findByRole('button', { name: 'Taslak oluştur' }));
    expect(await screen.findByText('Fasoncu seçin.')).toBeInTheDocument();
  });

  it(
    'reçete satırı eklenip silinir, en az bir satır kalır ve toplam canlı güncellenir',
    async () => {
      server.use(...createHandlers('MANAGER'));
      renderApp('/panel/sevkiyat/yeni');
      await fillRecipe('1.000', false);
      await userEvent.click(screen.getByRole('button', { name: 'Satır ekle' }));
      await userEvent.type(screen.getByLabelText(/Net kg 2/), '500,5');
      expect(screen.getByTestId('recipe-total')).toHaveTextContent('1.500,5 kg');
      await userEvent.click(screen.getByRole('button', { name: '2. satırı sil' }));
      expect(screen.queryByLabelText(/Net kg 2/)).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: '1. satırı sil' })).not.toBeInTheDocument();
    },
    LONG_FLOW_TIMEOUT_MS,
  );

  it('geçersiz reçete satırı ve fire oranını engeller', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat/yeni');
    await pick('Alıcı fasoncu', 'Bursa Tekstil');
    await userEvent.clear(screen.getByLabelText(/Fire oranı/));
    await userEvent.type(screen.getByLabelText(/Fire oranı/), '25');
    await userEvent.click(screen.getByRole('button', { name: 'Taslak oluştur' }));
    expect(
      await screen.findByText('Her satırda iplik cinsini listeden seçin.'),
    ).toBeInTheDocument();
    expect(screen.getByText(/Fire oranı 0 ile 20 arasında/)).toBeInTheDocument();
  });

  it('şoför listesi seçilen fasoncuya göre daralır', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat/yeni');
    await pick('Alıcı fasoncu', 'Aydın Örme');
    await userEvent.click(await screen.findByRole('combobox', { name: /Şoför/ }));
    expect(await screen.findByRole('option', { name: 'Şahin Şoför' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'Mert Kaya' })).not.toBeInTheDocument();
  });

  it('fasoncu seçilince şoför listesine merkez şoförleri de gelir', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat/yeni');
    await pick('Alıcı fasoncu', 'Aydın Örme');
    await userEvent.click(await screen.findByRole('combobox', { name: /Şoför/ }));
    expect(await screen.findByRole('option', { name: 'Merkez Mehmet' })).toBeInTheDocument();
    expect(screen.getByText('Merkez şoförleri')).toBeInTheDocument();
  });

  it('uyarı gösterilen alan düzelince "Taslak oluştur"a basmadan uyarı kalkar', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat/yeni');
    await userEvent.click(await screen.findByRole('button', { name: 'Taslak oluştur' }));
    expect(await screen.findByText('Fasoncu seçin.')).toBeInTheDocument();
    await pick('Alıcı fasoncu', 'Aydın Örme');
    expect(screen.queryByText('Fasoncu seçin.')).not.toBeInTheDocument();
  });

  it('gönderen fasoncuda iplik yoksa sunucunun reddi eksik kiloyla gösterilir', async () => {
    server.use(
      http.post('/api/v1/shipments', () =>
        HttpResponse.json(
          {
            status: 409,
            code: 'SOURCE_YARN_SHORTAGE',
            title: 'x',
            shortfalls: [{ yarnType: '20 denye likra', missingKg: '36.000' }],
          },
          { status: 409 },
        ),
      ),
      ...createHandlers('MANAGER'),
    );
    renderApp('/panel/sevkiyat/yeni');
    await pick('Alıcı fasoncu', 'Bursa Tekstil');
    await fillRecipe('36');
    await userEvent.click(screen.getByRole('button', { name: 'Taslak oluştur' }));
    expect(await screen.findByText(/elinde yeterli iplik yok/)).toBeInTheDocument();
    expect(screen.getByText(/20 denye likra: 36 kg eksik/)).toBeInTheDocument();
  });

  it('depo yeni sevkiyat açamaz', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat');
    await screen.findByText('SVK-2026-0001');
    expect(screen.queryByRole('link', { name: 'Yeni sevkiyat' })).not.toBeInTheDocument();
  });

  it('SHIPMENT_CREATE yetkisi olmayan rolde buton görünmez', async () => {
    server.use(...createHandlers('QUALITY'));
    renderApp('/panel/sevkiyat');
    await screen.findByText('SVK-2026-0001');
    expect(screen.queryByRole('link', { name: 'Yeni sevkiyat' })).not.toBeInTheDocument();
  });
});
