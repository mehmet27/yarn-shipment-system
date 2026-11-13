import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

describe('Merkez iplik stoku', () => {
  it('depo iplik girişi yapar, stok güncellenir; kumaş formunu görmez', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    const user = userEvent.setup();
    renderApp('/panel/merkez-stok');
    expect(await screen.findByText('Henüz iplik girişi yok.')).toBeInTheDocument();
    expect(screen.queryByRole('tab', { name: 'Kumaş girişi' })).not.toBeInTheDocument();

    await user.click(screen.getByRole('tab', { name: 'İplik girişi' }));
    const form = screen.getByRole('region', { name: 'Merkeze iplik girişi' });
    await user.type(within(form).getByLabelText(/İplik cinsi/), '30/1 Penye');
    await user.type(within(form).getByLabelText(/Gelen net kg/), '20000');
    await user.click(within(form).getByRole('button', { name: 'İplik girişini kaydet' }));

    expect(await screen.findByText('İplik girişi kaydedildi.')).toBeInTheDocument();
    await user.click(screen.getByRole('tab', { name: 'Eldeki iplik' }));
    const table = await screen.findByRole('table', { name: 'Merkez iplik stoku' });
    expect(within(table).getByText('30/1 Penye')).toBeInTheDocument();
  });

  it('kalite kumaş girer; iplik fire ile stoktan düşer', async () => {
    server.use(...createHandlers('ADMIN'));
    const user = userEvent.setup();
    renderApp('/panel/merkez-stok');
    await user.click(await screen.findByRole('tab', { name: 'İplik girişi' }));
    const receipt = await screen.findByRole('region', { name: 'Merkeze iplik girişi' });
    await user.type(within(receipt).getByLabelText(/İplik cinsi/), 'Penye');
    await user.type(within(receipt).getByLabelText(/Gelen net kg/), '1000');
    await user.click(within(receipt).getByRole('button', { name: 'İplik girişini kaydet' }));
    await screen.findByText('İplik girişi kaydedildi.');
    await user.click(screen.getByRole('tab', { name: 'Kumaş girişi' }));

    const form = screen.getByRole('region', { name: 'Merkez kumaş girişi' });
    await user.type(within(form).getByLabelText(/Kumaş cinsi/), 'Süprem');
    await user.type(within(form).getByLabelText('İplik cinsi 1'), 'Penye');
    await user.type(within(form).getByLabelText('Kumaş kg 1'), '480');
    await user.click(within(form).getByRole('button', { name: 'Kumaşı kaydet' }));

    await screen.findByText('Kumaş girişi kaydedildi.');
    await user.click(screen.getByRole('tab', { name: 'Eldeki iplik' }));
    const table = await screen.findByRole('table', { name: 'Son hareketler' });
    expect(within(table).getByText('Merkez kumaş üretimi')).toBeInTheDocument();
    expect(within(table).getByText(/−500/)).toBeInTheDocument();
  });

  it('müdür stoğu görür ama giriş formu yoktur; genel müdür menüde görmez', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/merkez-stok');
    expect(await screen.findByRole('heading', { name: 'Merkez iplik stoku' })).toBeInTheDocument();
    expect(screen.queryByRole('tab', { name: 'İplik girişi' })).not.toBeInTheDocument();
    expect(screen.queryByRole('tab', { name: 'Kumaş girişi' })).not.toBeInTheDocument();
  });
});
