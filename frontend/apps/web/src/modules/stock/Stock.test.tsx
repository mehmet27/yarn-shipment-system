import { ME_PATH } from '@iplik/api-client';
import { screen, within } from '@testing-library/react';
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
    await user.type(within(form).getByLabelText(/Gelen net kg/), '20000');
    await user.click(within(form).getByRole('button', { name: 'İplik girişini kaydet' }));

    expect(await screen.findByText('İplik girişi kaydedildi.')).toBeInTheDocument();
    await user.click(screen.getByRole('tab', { name: 'Eldeki iplik' }));
    const table = await screen.findByRole('table', { name: 'Merkez iplik stoku' });
    expect(within(table).getByText('Polyester 30/1')).toBeInTheDocument();
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
    await user.type(within(receipt).getByLabelText(/Gelen net kg/), '1000');
    await user.click(within(receipt).getByRole('button', { name: 'İplik girişini kaydet' }));
    await screen.findByText('İplik girişi kaydedildi.');
    await user.click(screen.getByRole('tab', { name: 'Kumaş girişi' }));

    const form = screen.getByRole('region', { name: 'Merkez kumaş girişi' });
    await user.type(within(form).getByLabelText(/Kumaş cinsi/), 'Süprem');
    await pickYarn(user, 'İplik cinsi 1', 'Pamuk 20/1');
    await user.type(within(form).getByLabelText('Kumaş kg 1'), '480');
    await user.click(within(form).getByRole('button', { name: 'Kumaşı kaydet' }));

    await screen.findByText('Kumaş girişi kaydedildi.');
    await user.click(screen.getByRole('tab', { name: 'Eldeki iplik' }));
    const table = await screen.findByRole('table', { name: 'Son hareketler' });
    expect(within(table).getByText('Merkez kumaş üretimi')).toBeInTheDocument();
    expect(within(table).getByText(/−500/)).toBeInTheDocument();
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

  it('müdür stoğu görür ama giriş formu yoktur; genel müdür menüde görmez', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/merkez-stok');
    expect(await screen.findByRole('heading', { name: 'Merkez iplik stoku' })).toBeInTheDocument();
    expect(screen.queryByRole('tab', { name: 'İplik girişi' })).not.toBeInTheDocument();
    expect(screen.queryByRole('tab', { name: 'Kumaş girişi' })).not.toBeInTheDocument();
  });
});
