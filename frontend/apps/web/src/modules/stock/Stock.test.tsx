import { saveMockRecipe } from '@/mocks/settlements';
import { ME_PATH } from '@iplik/api-client';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { createStockHandlers } from '@/mocks/stockHandlers';
import { server } from '@/mocks/server';
import { createMockUser } from '@/mocks/users';
import { renderApp } from '@/test/renderApp';

/** Tanımlar'daki iplik listesinden seçim; serbest metin yoktur. */
async function pickYarn(
  user: ReturnType<typeof userEvent.setup>,
  label: string | RegExp,
  name: string,
) {
  const inputs = await screen.findAllByLabelText(label);
  const input = inputs.at(0);
  if (input === undefined) throw new Error(`Seçici bulunamadı: ${String(label)}`);
  await user.click(input);
  await user.click(await screen.findByRole('option', { name }));
}

const SUPPLIER = /^Tedarikçi(\s*\*)?$/;

describe('Merkez iplik stoku', () => {
  it('depo iplik girişi yapar, stok güncellenir; kumaş formunu görmez', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    const user = userEvent.setup();
    renderApp('/panel/merkez-stok');
    expect(await screen.findByText('Henüz iplik girişi yok.')).toBeInTheDocument();
    expect(screen.queryByRole('tab', { name: 'Kumaş girişi' })).not.toBeInTheDocument();

    await user.click(screen.getByRole('tab', { name: 'İplik girişi' }));
    const form = screen.getByRole('region', { name: 'Merkeze iplik girişi' });
    await pickYarn(user, /İplik cinsi/, 'Polyester 30/1');
    await pickYarn(user, SUPPLIER, 'Şireci');
    await user.type(within(form).getByLabelText(/Gelen net kg/), '20000');
    await user.type(within(form).getByLabelText(/Tedarikçi irsaliye no/), 'IRS-7');
    await user.click(within(form).getByRole('button', { name: 'İplik girişini kaydet' }));

    expect(await screen.findByText('İplik girişi kaydedildi.')).toBeInTheDocument();
    await user.click(screen.getByRole('tab', { name: 'Eldeki iplik' }));
    const table = await screen.findByRole('table', { name: 'Merkez iplik stoku' });
    expect(within(table).getByText('Polyester 30/1')).toBeInTheDocument();
    const suppliers = await screen.findByRole('table', { name: 'Tedarikçiden gelen iplik' });
    expect(within(suppliers).getByText('Şireci')).toBeInTheDocument();
    expect(within(suppliers).getAllByText('20.000 kg')).toHaveLength(2);
    const moves = screen.getByRole('table', { name: 'Son hareketler' });
    expect(within(moves).getByText('IRS-7')).toBeInTheDocument();
    expect(screen.getByText('Gelen ipliğin dağılımı')).toBeInTheDocument();
    for (const ad of ['Toplam gelen iplik', 'Fasoncularda', 'Kapıdan aktarılan']) {
      expect(screen.getByText(ad)).toBeInTheDocument();
    }
  });

  it('kalite kumaş girer; iplik fire ile stoktan düşer', async () => {
    // İplik girişi (depo) ile kumaş girişi (kalite) farklı rollerdedir; akışı tek ekranda denemek için iki izni de taşıyan sahte kullanıcı.
    const quality = createMockUser('QUALITY');
    const both = {
      ...quality,
      permissions: [...quality.permissions, 'CENTRAL_STOCK_RECORD' as const],
    };
    server.use(
      http.get(ME_PATH, () => HttpResponse.json(both)),
      ...createStockHandlers(both),
      ...createHandlers('QUALITY'),
    );
    const user = userEvent.setup();
    renderApp('/panel/merkez-stok');
    await user.click(await screen.findByRole('tab', { name: 'İplik girişi' }));
    const receipt = await screen.findByRole('region', { name: 'Merkeze iplik girişi' });
    await pickYarn(user, /İplik cinsi/, 'Pamuk 20/1');
    await pickYarn(user, SUPPLIER, 'Karesi');
    await user.type(within(receipt).getByLabelText(/Gelen net kg/), '1000');
    await user.click(within(receipt).getByRole('button', { name: 'İplik girişini kaydet' }));
    await screen.findByText('İplik girişi kaydedildi.');
    await user.click(screen.getByRole('tab', { name: 'Kumaş girişi' }));

    const form = screen.getByRole('region', { name: 'Merkez kumaş girişi' });
    await user.type(within(form).getByRole('combobox', { name: /Kumaş cinsi/ }), 'Süprem');
    await user.type(within(form).getByLabelText(/Toplam kumaş kg/), '480');
    await pickYarn(user, 'İplik cinsi 1', 'Pamuk 20/1');
    await user.type(within(form).getByLabelText('Oran (%) 1'), '100');
    await user.click(within(form).getByRole('button', { name: 'Kumaşı kaydet' }));

    await screen.findByText('Kumaş girişi kaydedildi.');
    await user.click(screen.getByRole('tab', { name: 'Eldeki iplik' }));
    const table = await screen.findByRole('table', { name: 'Son hareketler' });
    expect(within(table).getByText('Merkez kumaş üretimi')).toBeInTheDocument();
    expect(within(table).getByText(/−500/)).toBeInTheDocument();
  });

  it('kumaş cinsi değişince iplikler ve oranlar tanımlı karışıma göre değişir', async () => {
    saveMockRecipe(undefined, {
      fabricType: 'Süprem',
      yarns: [{ yarnType: 'Pamuk 20/1', ratioPct: '100.00' }],
    });
    saveMockRecipe(undefined, {
      fabricType: 'İnterlok',
      yarns: [
        { yarnType: 'Polyester 30/1', ratioPct: '60.00' },
        { yarnType: 'Pamuk 20/1', ratioPct: '40.00' },
      ],
    });
    server.use(...createHandlers('QUALITY'));
    const user = userEvent.setup();
    renderApp('/panel/merkez-stok');
    await user.click(await screen.findByRole('tab', { name: 'Kumaş girişi' }));
    const form = screen.getByRole('region', { name: 'Merkez kumaş girişi' });
    const fabric = within(form).getByRole('combobox', { name: /Kumaş cinsi/ });

    await pickYarn(user, /Kumaş cinsi/, 'Süprem');
    await waitFor(() => {
      expect(within(form).getByLabelText('Oran (%) 1')).toHaveValue('100');
    });

    await user.clear(fabric);
    await pickYarn(user, /Kumaş cinsi/, 'İnterlok');
    await waitFor(() => {
      expect(within(form).getByLabelText('Oran (%) 2')).toHaveValue('40');
    });
    expect(within(form).getByLabelText('Oran (%) 1')).toHaveValue('60');

    await user.clear(fabric);
    await user.type(fabric, 'Başka');
    expect(within(form).getByLabelText('Oran (%) 1')).toHaveValue('');
    expect(within(form).queryByLabelText('Oran (%) 2')).not.toBeInTheDocument();
  });

  it('iplik seçilmeden giriş kaydedilmez; serbest metin alanı yoktur', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    const user = userEvent.setup();
    renderApp('/panel/merkez-stok');
    await user.click(await screen.findByRole('tab', { name: 'İplik girişi' }));
    const form = screen.getByRole('region', { name: 'Merkeze iplik girişi' });
    expect(within(form).getByText(/listede yoksa müdür Tanımlar/)).toBeInTheDocument();
    await user.type(within(form).getByLabelText(/Gelen net kg/), '100');
    await user.click(within(form).getByRole('button', { name: 'İplik girişini kaydet' }));

    expect(await screen.findByText('Zorunlu alanları doldurun.')).toBeInTheDocument();
    expect(screen.queryByText('İplik girişi kaydedildi.')).not.toBeInTheDocument();
  });

  it('iplik girişi düğmesine çift tıklanınca tek kayıt yazılır', async () => {
    let posts = 0;
    server.use(
      http.post('/api/v1/central-stock/yarn-receipts', () => {
        posts += 1;
        return HttpResponse.json(
          {
            id: 'x',
            productId: 'prd-0',
            yarnType: 'Polyester 30/1',
            kind: 'YARN_RECEIPT',
            netKg: '10.000',
            occurredAt: '2026-11-13T09:00:00Z',
          },
          { status: 201 },
        );
      }),
      ...createHandlers('WAREHOUSE'),
    );
    const user = userEvent.setup();
    renderApp('/panel/merkez-stok');
    await user.click(await screen.findByRole('tab', { name: 'İplik girişi' }));
    const form = screen.getByRole('region', { name: 'Merkeze iplik girişi' });
    await pickYarn(user, /İplik cinsi/, 'Polyester 30/1');
    await pickYarn(user, SUPPLIER, 'Şireci');
    await user.type(within(form).getByLabelText(/Gelen net kg/), '10');
    await user.dblClick(within(form).getByRole('button', { name: 'İplik girişini kaydet' }));
    // Yanıt çok hızlı gelirse ikinci tıklama boşalmış forma düşer; önemli olan tek kayıt yazılmasıdır.
    await waitFor(() => {
      expect(posts).toBe(1);
    });

    await new Promise((resolve) => setTimeout(resolve, 300));
    expect(posts).toBe(1);
  });

  it('tedarikçi seçilmeden giriş kaydedilmez', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    const user = userEvent.setup();
    renderApp('/panel/merkez-stok');
    await user.click(await screen.findByRole('tab', { name: 'İplik girişi' }));
    const form = screen.getByRole('region', { name: 'Merkeze iplik girişi' });
    await pickYarn(user, /İplik cinsi/, 'Polyester 30/1');
    await user.type(within(form).getByLabelText(/Gelen net kg/), '100');
    await user.click(within(form).getByRole('button', { name: 'İplik girişini kaydet' }));

    expect(await screen.findByText('Zorunlu alanları doldurun.')).toBeInTheDocument();
    expect(screen.queryByText('İplik girişi kaydedildi.')).not.toBeInTheDocument();
  });

  it('parti no yazılınca kalite sınıfı sorulur; geçersiz parti no gönderilmez', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    const user = userEvent.setup();
    renderApp('/panel/merkez-stok');
    await user.click(await screen.findByRole('tab', { name: 'İplik girişi' }));
    const form = screen.getByRole('region', { name: 'Merkeze iplik girişi' });
    expect(within(form).queryAllByLabelText(/Kalite sınıfı/)).toHaveLength(0);
    await pickYarn(user, /İplik cinsi/, 'Polyester 30/1');
    await pickYarn(user, SUPPLIER, 'Şireci');
    await user.type(within(form).getByLabelText(/Gelen net kg/), '100');

    await user.type(within(form).getByRole('combobox', { name: /Parti no/ }), 'ğ*');
    expect(within(form).getAllByLabelText(/Kalite sınıfı/).length).toBeGreaterThan(0);
    await user.click(within(form).getByRole('button', { name: 'İplik girişini kaydet' }));
    expect(await screen.findByText(/Parti no harf veya rakamla başlamalı/)).toBeInTheDocument();

    await user.clear(within(form).getByRole('combobox', { name: /Parti no/ }));
    await user.type(within(form).getByRole('combobox', { name: /Parti no/ }), 'p-1');
    await user.click(within(form).getByRole('button', { name: 'İplik girişini kaydet' }));
    expect(
      await screen.findByText(/ilk kez giriliyor; partinin kalite sınıfını seçin/),
    ).toBeInTheDocument();

    await pickYarn(user, /Kalite sınıfı/, 'A · Birinci kalite');
    await user.click(within(form).getByRole('button', { name: 'İplik girişini kaydet' }));
    expect(await screen.findByText('İplik girişi kaydedildi.')).toBeInTheDocument();
  });

  it('kayıtlı parti listeden seçilir; kalite sınıfı sorulmaz', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    const user = userEvent.setup();
    renderApp('/panel/merkez-stok');
    await user.click(await screen.findByRole('tab', { name: 'İplik girişi' }));
    const form = screen.getByRole('region', { name: 'Merkeze iplik girişi' });
    await pickYarn(user, /İplik cinsi/, 'Polyester 30/1');
    await pickYarn(user, SUPPLIER, 'Şireci');
    await user.type(within(form).getByLabelText(/Gelen net kg/), '100');
    await pickYarn(user, /Parti no/, 'P-2026-0001');

    expect(await within(form).findByText('Kayıtlı parti.')).toBeInTheDocument();
    expect(within(form).queryAllByLabelText(/Kalite sınıfı/)).toHaveLength(0);
    await user.click(within(form).getByRole('button', { name: 'İplik girişini kaydet' }));
    expect(await screen.findByText('İplik girişi kaydedildi.')).toBeInTheDocument();
  });

  it('müdür stoğu görür ama giriş formu yoktur; genel müdür menüde görmez', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/merkez-stok');
    expect(await screen.findByRole('heading', { name: 'Merkez iplik stoku' })).toBeInTheDocument();
    expect(screen.queryByRole('tab', { name: 'İplik girişi' })).not.toBeInTheDocument();
    expect(screen.queryByRole('tab', { name: 'Kumaş girişi' })).not.toBeInTheDocument();
  });
});
